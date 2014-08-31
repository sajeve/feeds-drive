package dh.newspaper.workflow;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;
import android.view.View;
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
import dh.tool.common.PerfWatcher;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.OncePrifoTask;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CancellationException;

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
public class SelectArticleWorkflow extends OncePrifoTask implements Comparable {
	private static final String TAG = SelectArticleWorkflow.class.getName();
	private static final Logger log = LoggerFactory.getLogger(SelectArticleWorkflow.class);

	@Inject DaoSession daoSessionReadonly;
	@Inject ContentParser mContentParser;
	//@Inject MessageDigest mMessageDigest;
	@Inject RefData refData;

	private final Duration mArticleTimeToLive;
	private final boolean mOnlineMode;
	//private final boolean mDownloadOriginal;
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

	//private Stopwatch mStopwatch;
	private PerfWatcher pw;

	private SelectArticleWorkflow(Context context, Duration articleTimeToLive, boolean online, SelectArticleCallback callback) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mArticleTimeToLive = articleTimeToLive;
		mCallback = callback;
		mOnlineMode = online;
	}

	/**
	 * Create workflow from a {@link dh.newspaper.model.FeedItem}
	 */
	public SelectArticleWorkflow(Context context, FeedItem feedItem, Duration articleTimeToLive, boolean online, SelectArticleCallback callback) {
		this(context, articleTimeToLive, online, callback);
		mFeedItem = feedItem;
		pw = new PerfWatcher(log, feedItem.getUri());
	}

	/**
	 * Create workflow from an existing {@link dh.newspaper.model.generated.Article}
	 */
	public SelectArticleWorkflow(Context context, Article article, Duration articleTimeToLive, boolean online, SelectArticleCallback callback) {
		this(context, articleTimeToLive, online, callback);
		mArticle = article;
		mFeedItem = new FeedItem(article.getParentUrl(), article.getTitle(), article.getPublishedDateString(), article.getExcerpt(), article.getArticleUrl(), article.getLanguage(), article.getAuthor());
		pw = new PerfWatcher(log, article.getArticleUrl());
	}

	//private DaoSession daoSessionReadonly;

	/**
	 * Start the workflow. This method can only be executed once. Otherwise, create other Workflow
	 */
	@Override
	public void perform() {
		pw.i("Start SelectArticleWorkflow");
		try {
			mParentSubscription = daoSessionReadonly.getSubscriptionDao().queryBuilder()
					.whereOr(SubscriptionDao.Properties.FeedsUrl.eq(mFeedItem.getParentUrl()),
							SubscriptionDao.Properties.FeedsUrl.eq(mFeedItem.getParentUrl()+"/"))
					.unique();
			pw.t("Found Parent subscription " + mParentSubscription);

			if (isCancelled()) {
				return;
			}

			if (mArticle == null) {
				//find mArticle from database
				mArticle = daoSessionReadonly.getArticleDao().queryBuilder()
						.where(ArticleDao.Properties.ArticleUrl.eq(mFeedItem.getUri())).unique();
				pw.t("Found Article in cache " + mArticle);
				if (isCancelled()) {
					return;
				}

				if (mCallback!=null) {
					mCallback.onFinishedCheckCache(this, getArticle());
					pw.t("Callback onFinishedCheckCache()");
				}

				//upsert the article
				if (mArticle == null) {
					insertNewToCache();
				}
				else {
					checkArticleExpirationThenUpdate();
				}
			}
			else { //mArticle is not null, refresh it from database
				try {
					daoSessionReadonly.getArticleDao().refresh(mArticle);
					pw.t("Refreshed cached Article from database " + mArticle);
					if (isCancelled()) {
						return;
					}

					if (mCallback!=null) {
						mCallback.onFinishedCheckCache(this, getArticle());
						pw.t("Callback onFinishedCheckCache()");
					}

					checkArticleExpirationThenUpdate();
				} catch (Exception ex) {
					Log.w(TAG, ex);
					mParseNotice.append(" Error while refresh cached article: " + ex.toString());
					return;
				}
			}

			getArticleContentDocument(); //parse the article content with jsoup if it is not parsed yet

		} finally {
			if (mCallback!=null) {
				mCallback.done(this, getArticle(), isCancelled());
				pw.t("callback done");
			}
			try {
				long duration = (new Duration(new DateTime(getStartTime()), DateTime.now())).getMillis();
				pw.ig("SelectArticleWorkflow completed " + duration + " ms" + (mSuccessDownloadAndExtraction ? " (dl and extract)" : ""));
			}
			catch (Exception ex) {
				Log.wtf(TAG, ex);
				pw.ig("SelectArticleWorkflow completed" + (mSuccessDownloadAndExtraction ? " (dl and extract)" : ""));
			}
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
		//region choose articleContent

		/*
		 * chose between
		 * mArticleContentDownloaded,  mFeedItem.getDescription
		 */
		String articleContent;
		if (mSuccessDownloadAndExtraction && !StrUtils.tooShort(mArticleTextPlainDownloaded, mFeedItem.getTextPlainDescription(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
			articleContent = mArticleContentDownloaded;
			pw.t("Choose downloaded content");
		}
		else {
			articleContent = mFeedItem.getDescription();
			mDoc = mFeedItem.getDocument();
			pw.t("Choose feed description");
			/*if (!mSuccessDownloadAndExtraction) {
				mParseNotice.append(" Downloaded content is too short, use feed description.");
			}*/
		}

		//endregion

		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}

		DateTime publishedDateTime = DateUtils.parseDateTime(mFeedItem.getPublishedDate());
		if (publishedDateTime==null) {
			if (Constants.DEBUG) {
				throw new IllegalStateException("Failed parse " + mFeedItem.getPublishedDate());
			}
			else {
				pw.w("Failed parse " + mFeedItem.getPublishedDate());
			}
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
				publishedDateTime==null ? DateTime.now().toDate() : publishedDateTime.toDate(),
				null,//date archive
				null,//last open
				DateTime.now().toDate(), //last updated
				mParseNotice.toString().trim(),
				mSuccessDownloadAndExtraction ? DateTime.now().toDate() : null //last update success
		);



		DaoMaster daoMaster = refData.createWritableDaoMaster();
		try {
			DaoSession daoSession = daoMaster.newSession();
			daoSession.getArticleDao().insert(mArticle);
		}
		catch (Exception ex) {
			pw.e("Failed to insert article to database", ex);
		}
		finally {
			try {
				daoMaster.getDatabase().close();
			}
			catch (Exception ex) {
				pw.e("Cannot close database", ex);
			}
		}
		pw.d("Insert new "+mArticle);

		//load image or in offline mode, replace image source by cache URL
		loadArticleImages();

		if (mCallback!=null && !isCancelled()) {
			mCallback.onFinishedUpdateCache(this, getArticle(), true);
			pw.t("Callback onFinishedUpdateCache()");
		}
	}

	private void checkArticleExpirationThenUpdate() {
		if (isCancelled()) {
			return;
		}
		boolean articleIsExpiry = mArticleTimeToLive==null || mArticle.getLastDownloadSuccess()==null || new Duration(new DateTime(mArticle.getLastDownloadSuccess()), DateTime.now()).isLongerThan(mArticleTimeToLive);

		if (articleIsExpiry) {
			updateArticleContent();
		}
		else {
			pw.trace("Article is not yet expiry ttl=" + mArticleTimeToLive + ". No need to re-download.");
		}
	}

	private void updateArticleContent() {
		downloadAndExtractArticleContent();

		if (isCancelled()) return;

		//region choose content

		if (mSuccessDownloadAndExtraction) {
			//choose content between mArticle and mArticleContentDownloaded
			if (StrUtils.tooShort(mArticleContentDownloaded, mArticle.getContent(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
				mParseNotice.append(" Downloaded content is too short (<"+Constants.ARTICLE_LENGTH_PERCENT_TOLERANT+"%), keep cache content unchanged.");
				mDoc = null; //invalidate Document
				pw.t("Keep cache content (downloaded content is too short)");
			} else {
				mArticle.setContent(mArticleContentDownloaded);
				pw.t("Choose downloaded content");
			}
			mArticle.setLastDownloadSuccess(DateTime.now().toDate());
		}
		else {
			//choose content between existed in node and feed description
			if (StrUtils.tooShort(mArticle.getContent(), mFeedItem.getDescription(), Constants.ARTICLE_LENGTH_PERCENT_TOLERANT)) {
				mParseNotice.append(" Cached content is too short, use feed description.");
				mArticle.setContent(mFeedItem.getDescription());
				pw.t("Choose feed description (cached content is too short)");
				mDoc = null;
			}
		}

		//endregion

		if (Strings.isNullOrEmpty(mArticleLanguage)) {
			mArticleLanguage = mFeedItem.getLanguage();
		}
		mArticle.setLanguage(mArticleLanguage);

		mArticle.setParseNotice(mParseNotice.toString().trim());
		mArticle.setLastUpdated(DateTime.now().toDate());

		pw.resetStopwatch();

		DaoMaster daoMaster = refData.createWritableDaoMaster();
		try {
			DaoSession daoSession = daoMaster.newSession();
			daoSession.getArticleDao().update(mArticle);
		}
		catch (Exception ex) {
			pw.e("Failed to update article in database", ex);
		}
		finally {
			try {
				daoMaster.getDatabase().close();
			}
			catch (Exception ex) {
				pw.e("Cannot close database", ex);
			}
		}
		pw.d("Update article content " + mArticle);

		//load image or in offline mode, replace image source by cache URL
		loadArticleImages(); //content changed, re-compute image

		if (mCallback!=null && !isCancelled()) {
			mCallback.onFinishedUpdateCache(this, getArticle(), false);
			pw.t("Callback onFinishedUpdateCache()");
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
			pw.d("No image found");
			return;
		}

		pw.d("Found "+elems.size()+ " images");

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
			pw.d("Set avatar "+avatar);
		}
		else {
			/**
			 * Mode offline: use image from cache instead
			 */
			if (Constants.DEBUG) {
				for (Element e : elems) {
					File imgFile = refData.getLruDiscCache().get(e.attr("abs:src"));
					if (imgFile != null) {
						e.attr("src", imgFile.getAbsolutePath());
						e.attr("style", "border:2px solid green;");
					}
				}
				pw.t("Change images border to recognise cached images");
			}
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
			pw.info("Null content");
			return null;
		}

		pw.resetStopwatch();
		mDoc = Jsoup.parse(mArticle.getContent());
		pw.d("Jsoup parse" + StrUtils.glimpse(mArticle.getContent()));
		return mDoc;
	}

	/**
	 * feed articleContent, mArticleLanguage, mParseNotice
	 */
	private void downloadAndExtractArticleContent() {
		if (!mOnlineMode) {
			pw.trace("Mode offline: use Feed Description as content");
			return;
		}

		if (isCancelled()) {
			return;
		}

		try {
			pw.resetStopwatch();

			//download content

			InputStream inputStream = NetworkUtils.getStreamFromUrl(mFeedItem.getUri(), NetworkUtils.DESKTOP_USER_AGENT, this);
			if (inputStream == null) {
				pw.trace("Download article content Cancelled");
				return;
			}
			pw.d("Content downloaded");

			extractContent(inputStream);
			mSuccessDownloadAndExtraction = true;
		} catch (IOException e) {
			mParseNotice.append(" IOException: "+e.toString());
			pw.w("IOException", e);
		} catch (Exception e) {
			mParseNotice.append(" Exception: "+e.toString());
			pw.w("Exception", e);
		}

		if (mCallback!=null && !isCancelled()) {
			mCallback.onFinishedDownloadContent(this, getArticle());
			pw.t("Callback onFinishedDownloadContent()");
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

		pw.resetStopwatch();

		/*if (mDownloadOriginal) {
			mDoc = Jsoup.parse(inputStream, encoding, mFeedItem.getUri());
		}
		else {
			mDoc = mContentParser.extractContent(inputStream, encoding, mFeedItem.getUri(), mParseNotice, this);
		}*/
		mDoc = mContentParser.extractContent(inputStream, encoding, mFeedItem.getUri(), mParseNotice, this);

		if (isCancelled()) return;

		mArticleContentDownloaded = mDoc.outerHtml();
		Element docBody = mDoc.body();
		mArticleTextPlainDownloaded = docBody == null ? null : docBody.text();

		if (Strings.isNullOrEmpty(mArticleTextPlainDownloaded)) {
			pw.t("Extract content done. Empty content: "+mParseNotice);
			mParseNotice.append(" Justext returns empty content.");
		}
		else {
			pw.t("Extract content done " + StrUtils.glimpse(mArticleContentDownloaded));
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
					null, mFeedItem.getLanguage(), null, mFeedItem.getPublishedDate(), null, null, null, null,
					"Fake article created from Feed Item", null);
		}
		return null;
	}

	public Subscription getParentSubscription() {
		return mParentSubscription;
	}

	public FeedItem getFeedItem() {
		return mFeedItem;
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

	@Override
	public String toString() {
		return String.format("[SelectArticleWorkflow: %s  successDownload = %s]", getFeedItem().getUri(), Boolean.toString(mSuccessDownloadAndExtraction));
	}

	@Override
	public String getMissionId() {
		return getArticleUrl();
	}

	private void checkNotOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			pw.warn("should not on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

	@Override
	public int compareTo(Object another) {
		//return thisPublishDate - anotherPublishDate
		Date anotherPublishedDate = ((SelectArticleWorkflow)another).getPublishedDate();
		Date publishedDate = getPublishedDate();
		if (publishedDate==null && anotherPublishedDate==null) {
			return 0;
		}
		if (anotherPublishedDate == null) {
			return -1;
		}
		if (publishedDate == null) {
			return 1;
		}
		return anotherPublishedDate.compareTo(publishedDate);
	}

	public Date getPublishedDate() {
		if (mArticle == null) {
			return null;
		}
		return mArticle.getPublishedDate();
	}

	public static interface SelectArticleCallback {
		public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article);
		public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article);
		public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew);
		public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled);
	}
}
