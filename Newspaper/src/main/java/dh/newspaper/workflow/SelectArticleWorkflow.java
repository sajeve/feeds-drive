package dh.newspaper.workflow;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.l3s.boilerpipe.extractors.ArticleExtractorNoTitle;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import de.l3s.boilerpipe.sax.HTMLHighlighter;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
	private final boolean mOnlineMode;
	private final SelectArticleCallback mCallback;

	private Article mArticle;
	private FeedItem mFeedItem;

	private String mArticleContentDownloaded;
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
		mOnlineMode = downloadFullContent;
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
				log("Find Parent subscription", mArticle);
				if (isCancelled()) {
					return;
				}

				if (mArticle == null) {
					//find mArticle from database
					mArticle = mDaoSession.getArticleDao().queryBuilder()
							.where(ArticleDao.Properties.ArticleUrl.eq(mFeedItem.getUri())).unique();
					log("Find Article in cache", mArticle);
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
						log("Refresh cached Article from database", mArticle);
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

				getArticleContentDocument(); //parse the article content with jsoup if it is not parsed yet

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
		downloadAndExtractArticleContent();
		if (isCancelled()) {
			return;
		}

		String articleContent;
		if (StrUtils.tooShort(mArticleContentDownloaded, mFeedItem.getDescription(), Constants.ARTICLE_LENGTH_TOLERANT)) {
			articleContent = mFeedItem.getDescription();
			mParseNotice.append(" Downloaded content is too short. Use feed description");
		}
		else {
			articleContent = mArticleContentDownloaded;
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
				articleContent,
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

		computeArticleImage();

		resetStopwatch();
		mDaoSession.getArticleDao().insert(mArticle);
		log("Insert new", mArticle);

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
		downloadAndExtractArticleContent();

		if (isCancelled()) {
			return;
		}

		String articleContent = Strings.isNullOrEmpty(mArticle.getContent()) ? mFeedItem.getDescription() : mArticle.getContent();

		if (StrUtils.tooShort(mArticleContentDownloaded, articleContent, Constants.ARTICLE_LENGTH_TOLERANT)) {
			mParseNotice.append(" Downloaded content is too short. Use old content");
		}
		else {
			articleContent = mArticleContentDownloaded;
		}

		mArticle.setContent(articleContent);

		if (mPathToContent!=null) {
			mArticle.setXpath(mPathToContent.getXpath());
		}
		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}
		mArticle.setLanguage(mArticleLanguage);

		mArticle.setParseNotice(mParseNotice.toString().trim());
		mArticle.setLastUpdated(DateTime.now().toDate());

		computeArticleImage();

		resetStopwatch();
		mDaoSession.getArticleDao().update(mArticle);
		log("Update article content", mArticle);

		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedUpdateCache(this, getArticle(), false);
			log("Callback onFinishedUpdateCache()");
		}
	}


	/**
	 * Update the avatar: put first valid image on article content to the {@link dh.newspaper.model.generated.Article#imageUrl}
	 * if there was no image from feeds description.
	 */
	private void computeArticleImage() {
		//if there is no image yet and the content downloaded from web is not null
		if (Strings.isNullOrEmpty(mArticle.getImageUrl()) && !Strings.isNullOrEmpty(mArticleContentDownloaded)) {
			if (getArticleContentDocument() != null) {
				mArticle.setImageUrl(ContentParser.findAvatar(getArticleContentDocument()));
			}
		}
	}


	Document mDoc;

	/**
	 * We parse article content only once time during the workflow running
	 * @return
	 */
	public Document getArticleContentDocument() {
		if (mDoc!=null) {
			return mDoc;
		}
		if (mArticle == null) {
			throw new IllegalStateException();
		}
		checkNotOnMainThread();
		if (Strings.isNullOrEmpty(mArticle.getContent())) {
			mDoc = null;
			logWarn("Null content");
			return null;
		}
		resetStopwatch();
		mDoc = Jsoup.parse(mArticle.getContent());
		log("Jsoup parse", StrUtils.glimpse(mArticle.getContent()));
		return mDoc;
	}

	/**
	 * feed articleContent, mArticleLanguage, mParseNotice
	 */
	private void downloadAndExtractArticleContent() {
		if (!mOnlineMode) {
			mArticleContentDownloaded = mFeedItem.getDescription();
			logSimple("Download Full content = false: use Feed Description as content");
			return;
		}

		if (isCancelled()) {
			return;
		}

		try {
			resetStopwatch();

			//download content
			byte[] data = NetworkUtils.downloadContent(mFeedItem.getUri(), NetworkUtils.MOBILE_USER_AGENT, this);
			if (data == null || data.length==0) {
				mParseNotice.append(" Raw content is null.");
				return;
			}
			log("Raw content downloaded");

			final HTMLDocument htmlDoc = new HTMLDocument(data, Charset.forName(Constants.DEFAULT_ENCODING));
			mArticleContentDownloaded = ContentParser.HTML_HIGHLIGHTER.process(htmlDoc, ContentParser.EXTRACTORS);

			log("Extract content done", StrUtils.glimpse(mArticleContentDownloaded));

			//articleContent = mContentParser.extractContent(inputStream, Constants.DEFAULT_ENCODING, mPathToContent.getXpath(), mFeedItem.getUri()).html();
			//mArticleContentDownloaded = mContentParser.getHtml(mContentParser.extractContent(inputStream, Constants.DEFAULT_ENCODING, mPathToContent.getXpath(), mFeedItem.getUri(), mParseNotice));
			//log("Download article content: "+StrUtils.glimpse(mArticleContentDownloaded));

			if (Strings.isNullOrEmpty(mArticleContentDownloaded)) {
				mParseNotice.append(" boilerpipe returns empty content.");
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

	private void logWarn(String message) {
		Log.w(TAG, message + " - "  + mFeedItem.getUri());
	}
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
	private void log(String message, Object data) {
		Log.v(TAG, message + " ("+mStopwatch.elapsed(TimeUnit.MILLISECONDS)+" ms): "+data+" - " + mFeedItem.getUri());
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

	private void checkNotOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			logWarn("should not on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

	public static interface SelectArticleCallback {
		public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article);
		public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article);
		public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew);
		public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled);
	}
}
