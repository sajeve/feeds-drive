package dh.newspaper.workflow;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.View;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.tools.DateUtils;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.PrifoTask;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import android.util.Log;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.CancellationException;
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

	private Document mDoc;
	private String mArticleContentDownloaded;
	private String mArticleTextPlainDownloaded;
	private boolean mSuccessDownloadAndExtraction = false;

	private String mArticleLanguage;
	//private PathToContent mPathToContent;
	private StringBuilder mParseNotice = new StringBuilder();
	private Subscription mParentSubscription;

	private volatile boolean mRunning;
	private volatile boolean used = false;

	private Stopwatch mStopwatch;
	private final ReentrantLock lock = new ReentrantLock();
	//private PerfWatcher pw;

	private SelectArticleWorkflow(Context context, Duration articleTimeToLive, boolean online, SelectArticleCallback callback) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mArticleTimeToLive = articleTimeToLive;
		mCallback = callback;
		mOnlineMode = online;
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
						.whereOr(SubscriptionDao.Properties.FeedsUrl.eq(mFeedItem.getParentUrl()),
								SubscriptionDao.Properties.FeedsUrl.eq(mFeedItem.getParentUrl()+"/"))
						.unique();
				log("Find Parent subscription", mParentSubscription);

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
						mParseNotice.append(" Error while refresh cached article: " + ex.toString());
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
				logInfo("Workflow complete (" + genericStopWatch.elapsed(TimeUnit.MILLISECONDS) + " ms)");
			}
		}catch (Exception ex) {
			Log.w(TAG, ex);
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
		if (StrUtils.tooShort(mArticleTextPlainDownloaded, mFeedItem.getTextPlainDescription(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
			articleContent = mFeedItem.getDescription();
			mParseNotice.append(" Downloaded content is too short. Use feed description");
		}
		else {
			articleContent = mArticleContentDownloaded;
		}

		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}

		DateTime publishedDateTime = DateUtils.parseDateTime(mFeedItem.getPublishedDate());

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
				publishedDateTime==null ? null : publishedDateTime.toDate(),
				null,//date archive
				null,//last open
				DateTime.now().toDate(), //last updated
				null, //mPathToContent == null ? null : mPathToContent.getXpath(), //xpath
				mParseNotice.toString().trim());

		resetStopwatch();
		mDaoSession.getArticleDao().insert(mArticle);
		log("Insert new", mArticle);

		//load image or in offline mode, replace image source by cache URL
		loadArticleImages();

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
		boolean articleIsExpiry = mArticleTimeToLive==null || mArticle.getLastUpdated()==null  || new Duration(new DateTime(mArticle.getLastUpdated()), DateTime.now()).isLongerThan(mArticleTimeToLive);

		if (articleIsExpiry) {
			updateArticleContent();
		}
		else {
			logSimple("Article is not yet expiry ttl="+mArticleTimeToLive+ ". No need to re-download");
		}
	}

	private void updateArticleContent() {
		downloadAndExtractArticleContent();

		if (isCancelled()) return;

		boolean contentIsChanged = true;
		if (mSuccessDownloadAndExtraction) {
			//choose content between mFeedItem.getDescription() and mArticleContentDownloaded
			String articleContent;
			if (StrUtils.tooShort(mArticleTextPlainDownloaded, mFeedItem.getTextPlainDescription(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
				mParseNotice.append(" Downloaded content is too short, use feed description.");
				mArticle.setContent(mFeedItem.getDescription());
			} else {
				mArticle.setContent(mArticleContentDownloaded);
			}
		}
		else {
			//choose content between existed in node and feed description
			if (StrUtils.tooShort(mArticle.getContent(), mFeedItem.getDescription(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
				mParseNotice.append(" Cached content is too short, use feed description.");
				mArticle.setContent(mFeedItem.getDescription());
			} else {
				contentIsChanged = false;
			}
		}

//		if (mPathToContent!=null) {
//			mArticle.setXpath(mPathToContent.getXpath());
//		}
		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}
		mArticle.setLanguage(mArticleLanguage);

		mArticle.setParseNotice(mParseNotice.toString().trim());
		mArticle.setLastUpdated(DateTime.now().toDate());

		resetStopwatch();
		mDaoSession.getArticleDao().update(mArticle);
		log("Update article content", mArticle);

		//load image or in offline mode, replace image source by cache URL
		loadArticleImages(); //content changed, re-compute image

		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedUpdateCache(this, getArticle(), false);
			log("Callback onFinishedUpdateCache()");
		}
	}

	/**
	 * load all image,
	 * set avatar = the middle image to {@link dh.newspaper.model.generated.Article#imageUrl}
	 *
	 * in offline mode, replace all image sources by the corresponding cache file, this method must not be called
	 * before save the article to the database. (Article image URL in the database must be the real one)
	 */
	private void loadArticleImages() {
		if (isCancelled() || getArticleContentDocument()==null) return;

		Elements elems = getArticleContentDocument().select("img");
		if (elems == null || elems.isEmpty()) {
			log("No image found");
			return;
		}

		log("Found "+elems.size()+ " images");

		if (mOnlineMode) {
			for (Element e : elems) {
				ImageLoader.getInstance().loadImage(e.attr("abs:src"), new ImageLoadingListener() {
					@Override
					public void onLoadingStarted(String imageUri, View view) {
						if (isCancelled()) {
							throw new CancellationException();
						}
					}

					@Override
					public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

					}

					@Override
					public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

					}

					@Override
					public void onLoadingCancelled(String imageUri, View view) {

					}
				});
			}

			String avatar = elems.get((elems.size() - 1) / 2).attr("abs:src");
			mArticle.setImageUrl(avatar);
			log("Set avatar", avatar);
		}
		else {
			/**
			 * Mode offline: use image from cache instead
			 */
			for (Element e : elems) {
				File imgFile = refData.getLruDiscCache().get(e.attr("abs:src"));
				if (imgFile!=null) {
					e.attr("src", imgFile.getAbsolutePath());
					e.attr("style", "border:2px solid green;");
				}
			}
			log("Change images border to recognise cached images");
		}
	}

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
			mArticleContentDownloaded = mArticle == null ? mFeedItem.getDescription() : mArticle.getContent();
			logSimple("Mode offline: use Feed Description as content");
			return;
		}

		if (isCancelled()) {
			return;
		}

		try {
			resetStopwatch();

			//download content

			InputStream inputStream = NetworkUtils.getStreamFromUrl(mFeedItem.getUri(), NetworkUtils.DESKTOP_USER_AGENT, this);
			if (inputStream == null) {
				log("Download article content Cancelled");
				return;
			}
			log("Content downloaded");

			extractContent(inputStream);

			mSuccessDownloadAndExtraction = true;
		} catch (IOException e) {
			mParseNotice.append(" IOException: "+e.toString());
			Log.w(TAG, e.toString());
		} catch (Exception e) {
			mParseNotice.append(" Exception: "+e.toString());
			Log.w(TAG, e);
		}

		if (mCallback!=null && !isCancelled()) {
			resetStopwatch();
			mCallback.onFinishedDownloadContent(this, getArticle());
			log("Callback onFinishedDownloadContent()");
		}
	}

	/**
	 * fill
	 * {@link #mArticleContentDownloaded}
	 * {@link #mArticleTextPlainDownloaded}
	 * {@link #mDoc}
	 */
	private void extractContent(InputStream inputStream) throws IOException {
		String encoding = Constants.DEFAULT_ENCODING;
		if (mParentSubscription != null && !Strings.isNullOrEmpty(mParentSubscription.getEncoding())) {
			encoding = mParentSubscription.getEncoding();
		}

		resetStopwatch();
		mDoc = mContentParser.extractContent(inputStream, encoding, mFeedItem.getUri(), mParseNotice, this);
		if (isCancelled()) return;

		mArticleContentDownloaded = mDoc.outerHtml();
		Element docBody = mDoc.body();
		mArticleTextPlainDownloaded = docBody == null ? null : docBody.text();


		if (Strings.isNullOrEmpty(mArticleTextPlainDownloaded)) {
			log("Extract content done", "Empty content: "+mParseNotice);
			mParseNotice.append(" Justext returns empty content.");
		}
		else {
			log("Extract content done", StrUtils.glimpse(mArticleContentDownloaded));
		}
	}

//	/**
//	 * feed mPathToContent
//	 */
//	private void findFirstMatchingPathToContent() {
//		if (isCancelled()) {
//			return;
//		}
//		resetStopwatch();
//
//		mPathToContent = findFirstMatchingPathToContent(mFeedItem.getUri());
//
//		log("First matching Path To Content is "+mPathToContent);
//	}
//
//	PathToContent findFirstMatchingPathToContent(String feedUri) {
//		for(PathToContent ptc : refData.pathToContentList()) {
//			if (feedUri.matches(ptc.getUrlPattern())) {
//				return ptc;
//			}
//		}
//		return null;
//	}

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
		Log.d(TAG, message + " - " + mFeedItem.getUri());
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
