package dh.newspaper.workflow;

import android.content.Context;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.nostra13.universalimageloader.core.ImageLoader;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.adapter.IArticleCollection;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.FeedParserException;
import dh.tool.thread.ICancellation;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoTask;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The workflow executed when user select a Tag.
 * This is also the collection of all article of the Tag.
 * It used the paging technique to freely navigate to any item in the big list
 * by holding a small articles list in memory (called buffer or page or windows). See {@link dh.newspaper.adapter.IArticleCollection}
 *
 * Created by hiep on 3/06/2014.
 */
public class SelectTagWorkflow extends PrifoTask implements IArticleCollection {
	private static final String TAG = SelectTagWorkflow.class.getName();

	@Inject DaoSession mDaoSession;
	@Inject ContentParser mContentParser;
	//@Inject MessageDigest mMessageDigest;
	//@Inject RefData refData;

	private final String mTag;
	private final Duration mSubscriptionsTimeToLive;
	private final Duration mArticleTimeToLive;
	private final PrifoExecutor mArticlesLoader;
	private final SelectTagCallback mCallback;
	private final Context mContext;

	private final ReentrantLock lock = new ReentrantLock();
	/**
	 * Special treatment for articleZero should be always in memory cache. Because the listView call for it all the time
	 */
	private Article mArticleZero;

	/**
	 * page cache
	 */
	private List<Article> mArticles;
	private int mOffset;
	private int mPageSize;
	private int mCountArticles;

	private HashMap<Subscription, Feeds> mSubscriptions;
	private QueryBuilder<Article> mSelectArticleQueryBuilder;

	/**
	 * use only if {@link dh.newspaper.workflow.SelectTagWorkflow#mArticlesLoader} is null,
	 * to run {@link dh.newspaper.workflow.SelectArticleWorkflow} on the same thread as this workflow.
	 */
	private SelectArticleWorkflow mCurrentLoadArticleWorkflow;
	private List<SelectArticleWorkflow> mPendingLoadArticleWorkflow = new ArrayList<SelectArticleWorkflow>();

	private List<String> mNotices = new ArrayList<String>();
	private Stopwatch mStopwatch;

	private final boolean mOnlineMode;

	private volatile boolean used = false;
	private volatile boolean mRunning = false;

	public SelectTagWorkflow(Context context, String tag, Duration subscriptionsTimeToLive, Duration articleTimeToLive, boolean onlineMode, int pageSize, PrifoExecutor articlesLoader, SelectTagCallback callback) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);

		mTag = tag;
		mSubscriptionsTimeToLive = subscriptionsTimeToLive;
		mArticleTimeToLive = articleTimeToLive;
		mContext = context;
		mArticlesLoader = articlesLoader;
		mCallback = callback;
		mPageSize = pageSize;
		mOnlineMode = onlineMode;
	}

	/**
	 * Start the workflow. This method can only be executed once. Otherwise, create other Workflow
	 */
	public void run() {
		if (isCancelled()) {
			logSimple(toString() + " is cancelled");
			return;
		}

		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (used) {
				logWarn(toString() + " is used");
				if (Constants.DEBUG) {
					throw new IllegalStateException(toString() + " is used");
				}
				return;
			}
			used = true;
			mRunning = true;

			logSimple("Start workflow");
			Stopwatch genericStopWatch = Stopwatch.createStarted();
			mStopwatch = Stopwatch.createStarted();
			try {
				loadFirstPageArticlesFromCache();
				if (mCallback != null && !isCancelled()) {
					resetStopwatch();
					mCallback.onFinishedLoadFromCache(this, mArticles, mCountArticles);
					log("Callback onFinishedLoadFromCache()");
				}

				if (mOnlineMode) {
					downloadFeeds();
					if (mCallback != null && !isCancelled()) {
						resetStopwatch();
						mCallback.onFinishedDownloadFeeds(this, mArticles, mCountArticles);
						log("Callback onFinishedDownloadFeeds()");
					}

					downloadArticles();
					if (mCallback != null && !isCancelled()) {
						resetStopwatch();
						mCallback.onFinishedDownloadArticles(this);
						log("Callback onFinishedDownloadArticles()");
					}
				}
				else {
					logSimple("Offline mode, only use cached articles");
				}
			}
			finally {
				if (mCallback!=null) {
					resetStopwatch();
					mCallback.done(this, mArticles, mCountArticles, mNotices, isCancelled());
					log("Callback done()");
				}
				mRunning = false;
				logInfo("Workflow complete ("+genericStopWatch.elapsed(TimeUnit.MILLISECONDS)+" ms)");
			}
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}
		finally {
			lock.unlock();
		}
	}

	private void loadFirstPageArticlesFromCache() {
		if (isCancelled()) {
			return;
		}
		logSimple("Load articles from cache");

		if (mSubscriptions == null || mSubscriptions.isEmpty()) {
			resetStopwatch();

			mSubscriptions = new HashMap<Subscription, Feeds>();

			for (Subscription sub : getSubscriptions(mTag)) {
				if (sub!=null) {
					mSubscriptions.put(sub, null);
				}
				else {
					logWarn("GreenDAO return null object");
				}
			}

			log("Found " + mSubscriptions.size() + " active subscriptions");
		}

		buildArticlesQuery();
		updateCountArticles();
		loadPage(0);
	}

	private boolean updateCountArticles() {
		if (isCancelled() || mSelectArticleQueryBuilder==null) {
			return false;
		}
		resetStopwatch();
		int oldCount = mCountArticles;
		mCountArticles = (int)mSelectArticleQueryBuilder.count();
		log("Count total articles = "+mCountArticles);

		return oldCount!=mCountArticles;
	}

	/**
	 * Load the page (or window) from the offset, the offset will be justify if out of allowing scope:
	 * (0..maxSize-pageSize)
	 * @param offset
	 * @return
	 */
	public boolean loadPage(int offset) {
		if (isCancelled()) {
			logWarn("The workflow is cancelled");//consider to throw exception here
			return false;
		}

		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			if (mSelectArticleQueryBuilder == null) {
				return false;
			}

			if (offset > mCountArticles-mPageSize) {
				offset = mCountArticles-mPageSize;
			}
			if (offset<0) {
				offset = 0;
			}

			mOffset = offset;

			resetStopwatch();

			checkAccessDiskOnMainThread();
			mArticles = mSelectArticleQueryBuilder.offset(mOffset).limit(mPageSize).list();

			log("loadPage("+offset+")");

			if (offset==0 && mArticles.size()>0) {
				//update the article-zero
				mArticleZero = mArticles.get(0);
			}

			if (mOnInMemoryCacheChangeCallback!=null) {
				mOnInMemoryCacheChangeCallback.onChanged(this, mArticles, mOffset, mPageSize);
			}

			return true;
		} finally {
			lock.unlock();
		}
	}

	private OnInMemoryCacheChangeCallback mOnInMemoryCacheChangeCallback;
	public void setCacheChangeListener(OnInMemoryCacheChangeCallback callback) {
		mOnInMemoryCacheChangeCallback = callback;
	}

/*
	public boolean loadNextPage() {
		return loadPage(mOffset + mCountArticles/2);
	}

	public boolean loadPreviousPage() {
		return loadPage(mOffset - mCountArticles/2);
	}

	public boolean isLastPage() {
		return mOffset + mPageSize >= mCountArticles;
	}
*/

	private void downloadFeeds() {
		if (isCancelled()) {
			return;
		}
		for(Subscription sub : getSubscriptions(mTag)) {
			if (isCancelled()) {
				return;
			}
			if (!isExpiry(sub)) {
				logSimple("Subscription is not yet expiry ttl="+mSubscriptionsTimeToLive+". No needs to re-download feeds list: " + sub);
				continue;
			}

			String encoding = sub.getEncoding();
			if (Strings.isNullOrEmpty(encoding)) {
				encoding = Constants.DEFAULT_ENCODING;
			}
			try {
				resetStopwatch();
				Feeds feeds = mContentParser.parseFeeds(sub.getFeedsUrl(), encoding, this);

				if (feeds == null) {
					log("Download and parse feeds cancelled "+sub);
					return;
				}

				log("Download and parse feeds of "+sub);

				mSubscriptions.put(sub, feeds);

				//upsert article to database with excerpt
				for (FeedItem feedItem : feeds) {
					if (isCancelled()) {
						return;
					}
					try {
						resetStopwatch();

						mCurrentLoadArticleWorkflow = new SelectArticleWorkflow(mContext, feedItem, mArticleTimeToLive, false, null);
						mCurrentLoadArticleWorkflow.run();

						log("Download full content of "+feedItem);
					}
					catch (Exception e) {
						logWarn(e);
						mNotices.add("Failed updating article excerpt "+feedItem+": "+e.getMessage());
					}
				}
			} catch (FeedParserException e) {
				logWarn(e);
				mNotices.add("Failed parsing feed of "+sub+": "+e.getMessage());
			} catch (IOException e) {
				logWarn(e);
				mNotices.add("Failed connect feed of "+sub+": "+e.getMessage());
			} catch (Exception e) {
				logWarn(e);
				mNotices.add("Fatal while parsing feed of "+sub+": "+e.getMessage());
			}
		}

		updateCountArticles();
		loadPage(mOffset);
	}

	private void downloadArticles() {
		if (isCancelled()) {
			return;
		}

		for(Subscription sub : mSubscriptions.keySet()) {
			if (isCancelled()) {
				return;
			}
			Feeds feeds = mSubscriptions.get(sub);

			if (feeds == null) {
				Log.d(TAG, "Feeds list is null "+sub);
				continue;
			}

			logSimple("Start download and parse full articles of " + sub);
			for (FeedItem feedItem : feeds) {
				if (isCancelled()) {
					return;
				}
				try {
					final SelectArticleWorkflow wkflow = new SelectArticleWorkflow(mContext, feedItem, mArticleTimeToLive, true, null);

					if (mArticlesLoader == null) {
						resetStopwatch();

						mCurrentLoadArticleWorkflow = wkflow;
						mCurrentLoadArticleWorkflow.run();

						log("Upsert excerpt "+feedItem);
					}
					else {
						logSimple("Add to article loader queue "+feedItem);
						mPendingLoadArticleWorkflow.add(wkflow);
						mArticlesLoader.execute(wkflow);
					}
				}
				catch (Exception e) {
					logWarn(e);
					mNotices.add("Failed updating article "+feedItem+": "+e.getMessage());
				}
			}
			logSimple("Finished download full articles of " + sub);

			resetStopwatch();

			if (!Strings.isNullOrEmpty(feeds.getDescription())) {
				sub.setDescription(feeds.getDescription());
			}
			if (!Strings.isNullOrEmpty(feeds.getLanguage())) {
				sub.setLanguage(feeds.getLanguage());
			}
			if (!Strings.isNullOrEmpty(feeds.getPubDate())) {
				sub.setPublishedDateString(feeds.getPubDate());
			}
			sub.setLastUpdate(DateTime.now().toDate());
			mDaoSession.getSubscriptionDao().update(sub);

			log("Update " + sub + " in database");
		}
	}

	private boolean isExpiry(Subscription sub) {
		if (mSubscriptionsTimeToLive == null || sub.getLastUpdate() == null) {
			return true;
		}
		return  new Duration(new DateTime(sub.getLastUpdate()), DateTime.now()).isLongerThan(mSubscriptionsTimeToLive);
	}

	private void buildArticlesQuery() {
		if (isCancelled()) {
			return;
		}
		if (mSelectArticleQueryBuilder == null) {
			resetStopwatch();

			mSelectArticleQueryBuilder = buildArticlesQuery(mSubscriptions.keySet().toArray(new Subscription[mSubscriptions.size()]));

			log("Build article query from " + mSubscriptions.size() + " subscriptions");
		}
	}

	/**
	 * Query to find all article related to a subscription list. Use to find all article of a given Tag
	 */
	public QueryBuilder<Article> buildArticlesQuery(Subscription[] subscriptions) {
		if (subscriptions == null || subscriptions.length==0) {
			return null;
		}

		QueryBuilder<Article> queryBuilder = mDaoSession.getArticleDao().queryBuilder();
		int subCount = subscriptions.length;
		if (subCount == 1) {
			queryBuilder.where(ArticleDao.Properties.ParentUrl.eq(subscriptions[0].getFeedsUrl()));
		}
		else if (subCount == 2) {
			queryBuilder.whereOr(
					ArticleDao.Properties.ParentUrl.eq(subscriptions[0].getFeedsUrl()),
					ArticleDao.Properties.ParentUrl.eq(subscriptions[1].getFeedsUrl())
			);
		}
		else { //subCount > 2
			WhereCondition[] whereConditions = new WhereCondition[subCount-2];
			for (int i = 2; i<subCount; i++) {
				whereConditions[i-2] = ArticleDao.Properties.ParentUrl.eq(subscriptions[i].getFeedsUrl());
			}

			queryBuilder.whereOr(
					ArticleDao.Properties.ParentUrl.eq(subscriptions[0].getFeedsUrl()),
					ArticleDao.Properties.ParentUrl.eq(subscriptions[1].getFeedsUrl()),
					whereConditions
			);
		}

		queryBuilder.orderDesc(ArticleDao.Properties.PublishedDate).orderDesc(ArticleDao.Properties.LastUpdated);

		return queryBuilder;
	}

	/**
	 * Find subscriptions by tag: Select * from Subscription where tag like "%|tag|%"
	 * @param tag
	 */
	public List<Subscription> getSubscriptions(String tag) {
		if (Strings.isNullOrEmpty(tag)) {
			QueryBuilder<Subscription> qb = mDaoSession.getSubscriptionDao().queryBuilder();
			qb.where(
					SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
					qb.or(SubscriptionDao.Properties.Tags.isNull(), SubscriptionDao.Properties.Tags.eq("")));

			checkAccessDiskOnMainThread();
			return qb.list();
		}

		checkAccessDiskOnMainThread();
		return mDaoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
						SubscriptionDao.Properties.Tags.like("%"+getTechnicalTag(tag)+"%")).list();
	}

	private String getTechnicalTag(String tag) {
		return "|"+StrUtils.normalizeUpper(tag)+"|";
	}

	@Override
	public void cancel() {
		super.cancel();
		if (mCurrentLoadArticleWorkflow != null) {
			mCurrentLoadArticleWorkflow.cancel();
		}
		if (mPendingLoadArticleWorkflow != null) {
			for (SelectArticleWorkflow saw : mPendingLoadArticleWorkflow) {
				saw.cancel();
			}
		}
		logSimple("Cancelled workflow");
	}

	@Override
	public boolean isInMemoryCache(int position) {
		if (position==0) {
			return true; //articleZero is always in memory
		}
		if (mArticles == null) {
			return false;
		}
		return mOffset <= position && position <= mOffset+ mPageSize - 1;
	}

	/**
	 * get article at any position, move to the window to the position.
	 * except the position 0 is special, getArticle(0) is called all the time, so it should return immediately the
	 * article zero and do not move the window.
	 */
	@Override
	public Article getArticle(int position) {
		if (mArticles == null || mArticles.size()==0) {
			return null;
		}
		if (position == 0) {
			if (mArticleZero==null) {
				mArticleZero = mArticles.get(0);
			}
			return mArticleZero;
		}

		if (isInMemoryCache(position)) {
			return mArticles.get(position-mOffset);
		}
		else {
			if (loadPage(position - mPageSize / 2)) {
				return mArticles.get(position-mOffset);
				/*if (position-mOffset<mArticles.size())
					return mArticles.get(position-mOffset);
				else {
					Log.w(TAG, "Too far position "+position);
				}*/
			}
			return null;
		}
	}

	public int getOffset() {
		return mOffset;
	}

	public List<Article> getInMemoryCache() {
		return mArticles;
	}
	@Override
	public int getTotalSize() {
		return mCountArticles;
	}

	public List<String> getNotices() {
		return mNotices;
	}

	public String getTag() {
		return mTag;
	}

	public boolean isRunning() {
		return mRunning;
	}

	//<editor-fold desc="Simple Log Utils for Profiler">

	private void logInfo(String message) {
		Log.d(TAG, message + " - " + mTag);
	}
	private void logWarn(String message) {
		Log.w(TAG, message + " - " + mTag);
	}
	private void logWarn(Throwable ex) {
		Log.w(TAG, "Error " + mTag, ex);
	}
	private void logSimple(String message) {
		Log.v(TAG, message + " - " + mTag);
	}
	private void log(String message) {
		Log.v(TAG, message + " ("+mStopwatch.elapsed(TimeUnit.MILLISECONDS)+" ms) - " + mTag);
		resetStopwatch();
	}
	private void resetStopwatch() {
		mStopwatch.reset().start();
	}

	//</editor-fold>

	@Override
	public String toString() {
		return String.format("[SelectTagWorkflow: %s]", mTag);
	}

	@Override
	public String getMissionId() {
		return getTag();
	}

	private void checkAccessDiskOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			logWarn("Access disk on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

	public static interface SelectTagCallback {
		public void onFinishedLoadFromCache(SelectTagWorkflow sender, List<Article> articles, int count);
		public void onFinishedDownloadFeeds(SelectTagWorkflow sender, List<Article> articles, int count);
		public void onFinishedDownloadArticles(SelectTagWorkflow sender);
		public void done(SelectTagWorkflow sender, List<Article> articles, int count, List<String> notices, boolean isCancelled);
	}
}
