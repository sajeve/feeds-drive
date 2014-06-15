package dh.newspaper.workflow;

import android.content.Context;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.tools.StrUtils;
import dh.newspaper.tools.thread.PrifoTask;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Workflow:
 *
 * Find if article exist in cache.
 * - if yes: update cache if article is out-date (articleTimeToLive)
 * - if no: insert it to cache
 *
 * if downloadFullContent = true: we download the full content of the article using the matching xpath
 * if downloadFullContent = false: we use Feed Description as content of the article
 *
 * Created by hiep on 3/06/2014.
 */
public class SelectArticleWorkflow extends PrifoTask {
	private static final String TAG = SelectArticleWorkflow.class.getName();

	@Inject DaoSession mDaoSession;
	@Inject ContentParser mContentParser;
	@Inject MessageDigest mMessageDigest;
	@Inject RefData refData;

	private final Duration mArticleTimeToLive;
	private final boolean mDownloadFullContent;
	private final SelectArticleCallback mCallback;

	private Article mArticle;
	private FeedItem mFeedItem;

	private String mArticleContent;
	private String mArticleLanguage;
	private PathToContent mPathToContent;
	private StringBuilder mParseNotice = new StringBuilder();
	private Subscription mParentSubscription;

	private volatile boolean mRunning;
	private volatile boolean used = false;

	private Stopwatch mStopwatch;
	private final ReentrantLock lock = new ReentrantLock();

	private SelectArticleWorkflow(Context context, Duration articleTimeToLive, boolean downloadFullContent, SelectArticleCallback callback) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mArticleTimeToLive = articleTimeToLive;
		mCallback = callback;
		mDownloadFullContent = downloadFullContent;
	}

	/**
	 * Create workflow from a {@link dh.newspaper.model.FeedItem}
	 */
	public SelectArticleWorkflow(Context context, FeedItem feedItem, Duration articleTimeToLive, boolean downloadFullContent, SelectArticleCallback callback) {
		this(context, articleTimeToLive, downloadFullContent, callback);
		mFeedItem = feedItem;
	}

	/**
	 * Create workflow from an existing {@link dh.newspaper.model.generated.Article}
	 */
	public SelectArticleWorkflow(Context context, Article article, Duration articleTimeToLive, boolean downloadFullContent, SelectArticleCallback callback) {
		this(context, articleTimeToLive, downloadFullContent, callback);
		mArticle = article;
		mFeedItem = new FeedItem(article.getParentUrl(), article.getTitle(), article.getPublishedDateString(), article.getExcerpt(), article.getArticleUrl(), article.getLanguage(), article.getAuthor());
	}

	/**
	 * Start the workflow. This method can only be executed once. Otherwise, create other Workflow
	 */
	@Override
	public void run() {
		if (isCancelled()) {
			logSimple(toString() + " is cancelled");
			return;
		}
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (used) {
				Log.w(TAG, toString()+" is used");
				if (Constants.DEBUG) {
					throw new IllegalStateException(toString() + " is used");
				}
				return;
			}
			used = true;
			mRunning = true;

			logSimple("Start SelectArticleWorkflow");
			Stopwatch genericStopWatch = Stopwatch.createStarted();
			mStopwatch = Stopwatch.createStarted();
			try {
				resetStopwatch();

				mParentSubscription = mDaoSession.getSubscriptionDao().queryBuilder()
						.where(SubscriptionDao.Properties.FeedsUrl.eq(mFeedItem.getParentUrl()))
						.unique();
				log("Find Parent subscription: "+mArticle);
				if (isCancelled()) {
					return;
				}

				if (mArticle == null) {
					//find mArticle from database
					mArticle = mDaoSession.getArticleDao().queryBuilder()
							.where(ArticleDao.Properties.ArticleUrl.eq(mFeedItem.getUri())).unique();
					log("Find Article in cache: "+mArticle);
					if (isCancelled()) {
						return;
					}

					if (mCallback!=null) {
						resetStopwatch();
						mCallback.onFinishedCheckCache(this, getArticle());
						log("Callback onFinishedCheckCache()");
					}

					//upsert the article
					if (mArticle == null) {
						insertNewToCache();
					}
					else {
						checkArticleExpiration();
					}
				}
				else { //mArticle is not null, refresh it from database
					try {
						mDaoSession.getArticleDao().refresh(mArticle);
						log("Refresh cached Article from database: "+mArticle);
						if (isCancelled()) {
							return;
						}

						if (mCallback!=null) {
							resetStopwatch();
							mCallback.onFinishedCheckCache(this, getArticle());
							log("Callback onFinishedCheckCache()");
						}

						checkArticleExpiration();
					} catch (Exception ex) {
						Log.w(TAG, ex);
						mParseNotice.append(" Error while refresh cached article: " + ex.getMessage());
						return;
					}
				}

			} finally {
				if (mCallback!=null) {
					logSimple("callback done");
					mCallback.done(this, getArticle(), isCancelled());
				}
				mRunning = false;
				logInfo("Workflow complete ("+genericStopWatch.elapsed(TimeUnit.MILLISECONDS)+" ms)");
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * mArticle=null
	 */
	private void insertNewToCache() {
		downloadArticleContent();
		if (isCancelled()) {
			return;
		}

		resetStopwatch();

		if (Strings.isNullOrEmpty(mArticleContent)) {
			mArticleContent = mFeedItem.getDescription();
			mParseNotice.append(" Use feed description as content");
		}

		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}

		mArticle = new Article(null, //id
				mFeedItem.getUri(),
				mFeedItem.getParentUrl(),
				mFeedItem.getImageUrl(),
				mFeedItem.getTitle(),
				mFeedItem.getAuthor(),
				mFeedItem.getExcerpt(),
				mArticleContent,
				null, //checksum
				mArticleLanguage,
				0L, //openedCount
				mFeedItem.getPublishedDate(),
				StrUtils.parseDateTime(mFeedItem.getPublishedDate()).toDate(),
				null,//date archive
				null,//last open
				DateTime.now().toDate(), //last updated
				mPathToContent == null ? null : mPathToContent.getXpath(), //xpath
				mParseNotice.toString().trim());

		mDaoSession.getArticleDao().insert(mArticle);
		log("Insert new "+mArticle);
		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedUpdateCache(this, getArticle(), true);
			log("Callback onFinishedUpdateCache()");
		}
	}

	private void checkArticleExpiration() {
		if (isCancelled()) {
			return;
		}
		boolean articleIsExpiry = (mArticleTimeToLive == null) || new Duration(new DateTime(mArticle.getLastUpdated()), DateTime.now()).isLongerThan(mArticleTimeToLive);

		if (articleIsExpiry) {
			updateArticleContent();
		}
		else {
			logSimple("Article is not yet expiry ttl="+mArticleTimeToLive+ ". No need to re-download");
		}
	}

	private void updateArticleContent() {
		downloadArticleContent();

		if (isCancelled()) {
			return;
		}
		resetStopwatch();

		if (Strings.isNullOrEmpty(mArticleContent)) {
			if (Strings.isNullOrEmpty(mArticle.getContent())) {
				mArticleContent = mFeedItem.getDescription();
				mParseNotice.append(" Use feed description as content");
			}
			else {
				mArticleContent = mArticle.getContent();
				mParseNotice.append(" Keep old content in cache");
			}
		}
		mArticle.setContent(mArticleContent);

		if (mPathToContent!=null) {
			mArticle.setXpath(mPathToContent.getXpath());
		}
		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}
		mArticle.setLanguage(mArticleLanguage);

		mArticle.setParseNotice(mParseNotice.toString().trim());
		mArticle.setLastUpdated(DateTime.now().toDate());

		mDaoSession.getArticleDao().update(mArticle);

		log("Update article content"+mArticle);

		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedUpdateCache(this, getArticle(), false);
			log("Callback onFinishedUpdateCache()");
		}
	}

	/**
	 * feed mArticleContent, mArticleLanguage, mParseNotice
	 */
	private void downloadArticleContent() {
		if (!mDownloadFullContent) {
			mArticleContent = mFeedItem.getDescription();
			logSimple("Download Full content = false: use Feed Description as content");
			return;
		}

		findFirstMatchingPathToContent();
		if (isCancelled()) {
			return;
		}
		if (mPathToContent == null) {
			mParseNotice.append(" XPath not found in "+PathToContentDao.TABLENAME);
			return;
		}
		mArticleLanguage = mPathToContent.getLanguage();
		try {
			resetStopwatch();
			//mArticleContent = mContentParser.extractContent(mFeedItem.getUri(), mPathToContent.getXpath()).html();

			InputStream inputStream = NetworkUtils.getStreamFromUrl(mFeedItem.getUri(), NetworkUtils.MOBILE_USER_AGENT, this);
			if (inputStream == null) {
				log("Download article content Cancelled");
				return;
			}

			mArticleContent = mContentParser.extractContent(inputStream, Constants.DEFAULT_ENCODING, mPathToContent.getXpath(), mFeedItem.getUri()).html();

			log("Download article content: "+StrUtils.glimpse(mArticleContent));
			if (Strings.isNullOrEmpty(mArticleContent)) {
				mParseNotice.append(" Empty content. Verify if the xpath matches page source");
			}
		} catch (IOException e) {
			mParseNotice.append(" IOException: "+e.getMessage());
			Log.w(TAG, e.toString());
		} catch (Exception e) {
			mParseNotice.append(" Fatal while extracting content: "+e.getMessage());
			Log.w(TAG, e);
		}

		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedDownloadContent(this, getArticle());
			log("Callback onFinishedDownloadContent()");
		}
	}

	/**
	 * feed mPathToContent
	 */
	private void findFirstMatchingPathToContent() {
		if (isCancelled()) {
			return;
		}
		resetStopwatch();

		mPathToContent = findFirstMatchingPathToContent(mFeedItem.getUri());

		log("First matching Path To Content is "+mPathToContent);
	}

	PathToContent findFirstMatchingPathToContent(String feedUri) {
		for(PathToContent ptc : refData.pathToContentList()) {
			if (feedUri.matches(ptc.getUrlPattern())) {
				return ptc;
			}
		}
		return null;
	}

	public Article getArticle() {
		if (mArticle != null) {
			return mArticle;
		}
		else if (mFeedItem!=null) {
			return new Article(null, mFeedItem.getUri(), mFeedItem.getParentUrl(), mFeedItem.getImageUrl(),
					mFeedItem.getTitle(), mFeedItem.getAuthor(), mFeedItem.getExcerpt(), mFeedItem.getDescription(),
					null, mFeedItem.getLanguage(), null, mFeedItem.getPublishedDate(), null, null, null, null, null,
					"Fake article created from Feed Item");
		}
		return null;
	}

	public Subscription getParentSubscription() {
		return mParentSubscription;
	}

	public FeedItem getFeedItem() {
		return mFeedItem;
	}

	public boolean isRunning() {
		return mRunning;
	}

	/**
	 * Use to identify this workflow
	 */
	public String getArticleUrl() {
		if (mFeedItem!=null) {
			return mFeedItem.getUri();
		}
		if (mArticle!=null) {
			return mArticle.getArticleUrl();
		}
		return null;
	}

	//<editor-fold desc="Simple Log Utils for Profiler">

	private void logInfo(String message) {
		Log.d(TAG, message + " - "  + mFeedItem.getUri());
	}
	private void logSimple(String message) {
		Log.v(TAG, message + " - " + mFeedItem.getUri());
	}
	private void log(String message) {
		Log.v(TAG, message + " ("+mStopwatch.elapsed(TimeUnit.MILLISECONDS)+" ms) - " + mFeedItem.getUri());
		mStopwatch.reset().start();
	}
	private void resetStopwatch() {
		mStopwatch.reset().start();
	}

	//</editor-fold>

	@Override
	public String toString() {
		return String.format("[SelectArticleWorkflow: %s]", getFeedItem().getUri());
	}

	@Override
	public String getMissionId() {
		return getArticleUrl();
	}

	public static interface SelectArticleCallback {
		public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article);
		public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article);
		public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew);
		public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled);
	}
}
