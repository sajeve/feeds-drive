package dh.newspaper.workflow;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import com.google.common.base.Strings;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.adapter.IArticleCollection;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.tools.TagUtils;
import dh.tool.common.PerfWatcher;
import dh.tool.thread.prifo.*;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The workflow executed when user select a Tag.
 * This is also the collection of all article of the Tag.
 * It used the paging technique to freely navigate to any item in the big list
 * by holding a small articles list in memory (called buffer or page or windows). See {@link dh.newspaper.adapter.IArticleCollection}
 *
 * Created by hiep on 3/06/2014.
 */
public class SelectTagWorkflow extends OncePrifoTask implements IArticleCollection {
	private static final String TAG = SelectTagWorkflow.class.getName();
	private static final Logger log = LoggerFactory.getLogger(SelectTagWorkflow.class);
	private final PerfWatcher pw;

	@Inject ContentParser mContentParser;
	//@Inject MessageDigest mMessageDigest;
	//@Inject RefData refData;

	DaoSession mDaoSessionReadonly;
	private final String mTag;
	private final Duration mSubscriptionsTimeToLive;
	private final Duration mArticleTimeToLive;
	private final PrifoExecutor mArticlesLoader;
	private final SelectTagCallback mCallback;
	private final Context mContext;
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

	//private Stopwatch mStopwatchQueue;
	private final boolean mOnlineMode;

	/**
	 *
	 * @param context
	 * @param tag
	 * @param subscriptionsTimeToLive
	 * @param articleTimeToLive
	 * @param onlineMode
	 * @param pageSize
	 * @param articlesLoader synchronise mode if null
	 * @param callback
	 */
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
		//mStopwatchQueue = mStopwatchQueue.createStarted();
		pw = new PerfWatcher(log, mTag);
	}

	//private DaoSession daoSessionReadonly;
	/**
	 * Start the workflow. This method can only be executed once. Otherwise, create other Workflow
	 */
	@Override
	public void perform() {
		pw.i("Start SelectTagWorkflow");
		DaoMaster daoMaster = mRefData.createReadOnlyDaoMaster();
		try {
			mDaoSessionReadonly = daoMaster.newSession();

			loadFirstPageArticlesFromCache();

			checkCancellation();

			if (mCallback!=null) {
				pw.resetStopwatch();
				mCallback.onFinishedLoadFromCache(this, mArticles, mCountArticles);
				pw.t("Callback onFinishedLoadFromCache()");
			}

			if (mOnlineMode) {
				downloadFeeds();

				checkCancellation();
				if (mCallback!=null) {
					pw.resetStopwatch();
					mCallback.onFinishedDownloadFeeds(this, mArticles, mCountArticles);
					pw.t("Callback onFinishedDownloadFeeds()");
				}

				downloadArticles();

				checkCancellation();
				if (mCallback!=null) {
					pw.resetStopwatch();
					mCallback.onFinishedDownloadArticles(this);
					pw.t("Callback onFinishedDownloadArticles()");
				}
			}
			else {
				pw.debug("Offline mode, only use cached articles");
			}
		}
		finally {
			if (mCallback!=null) {
				pw.resetStopwatch();
				mCallback.done(this, mArticles, mCountArticles, mNotices, isCancelled());
				pw.t("Callback done()");
			}

			daoMaster.getDatabase().close();

			try {
				long duration = (new Duration(new DateTime(getStartTime()), DateTime.now())).getMillis();
				pw.ig("SelectTagWorkflow completed " + duration + " ms");
			}
			catch (Exception ex) {
				Log.wtf(TAG, ex);
				pw.ig("SelectTagWorkflow completed");
			}
		}
	}

	private void loadFirstPageArticlesFromCache() {
		checkCancellation();
		pw.debug("Load articles from cache");

		if (mSubscriptions == null || mSubscriptions.isEmpty()) {
			pw.resetStopwatch();

			mSubscriptions = new HashMap<Subscription, Feeds>();

			for (Subscription sub : getSubscriptions(mTag)) {
				if (sub!=null) {
					mSubscriptions.put(sub, null);
				}
				else {
					pw.warn("GreenDAO return null object");
				}
			}

			pw.d("Found " + mSubscriptions.size() + " active subscriptions");
		}

		buildArticlesQuery();
		updateCountArticles();
		loadPage(0);
	}

	private boolean updateCountArticles() {
		if (isCancelled() || mSelectArticleQueryBuilder==null) {
			return false;
		}
		pw.resetStopwatch();
		int oldCount = mCountArticles;
		mCountArticles = (int)mSelectArticleQueryBuilder.count();
		pw.d("Count total articles = "+mCountArticles);

		return oldCount!=mCountArticles;
	}

	/**
	 * Load the page (or window) from the offset, the offset will be justify if out of allowing scope:
	 * (0..maxSize-pageSize)
	 * @param offset
	 * @return
	 */
	public boolean loadPage(int offset) {
		checkCancellation();

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

			pw.resetStopwatch();

			checkAccessDiskOnMainThread();
			mArticles = mSelectArticleQueryBuilder.offset(mOffset).limit(mPageSize).list();

			pw.t("loadPage(" + offset + ")");

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
		checkCancellation();
		for(Subscription sub : getSubscriptions(mTag)) {
			checkCancellation();
			if (!isExpiry(sub)) {
				pw.debug("Subscription is not yet expiry ttl="+mSubscriptionsTimeToLive+". No needs to re-download feeds list: " + sub);
				continue;
			}

			String encoding = sub.getEncoding();
			if (Strings.isNullOrEmpty(encoding)) {
				encoding = Constants.DEFAULT_ENCODING;
			}
			try {
				pw.resetStopwatch();
				Feeds feeds = mContentParser.parseFeeds(sub.getFeedsUrl(), encoding, this);

				if (feeds == null) {
					pw.d("Download and parse feeds cancelled " + sub);
					return;
				}

				pw.d("Download and parse feeds of " + sub);

				mSubscriptions.put(sub, feeds);

				//upsert article to database with excerpt
				for (FeedItem feedItem : feeds) {
					checkCancellation();
					try {
						pw.resetStopwatch();

						mCurrentLoadArticleWorkflow = new SelectArticleWorkflow(mContext, feedItem, mArticleTimeToLive, false, null);
						mCurrentLoadArticleWorkflow.run();

						pw.t("Download full content of " + feedItem);
					} catch (CancellationException e) {
						throw e;
					} catch (Exception e) {
						pw.warn("Failed updating article", e);
						mNotices.add("Failed updating article excerpt " + feedItem + ": " + e);
					}
				}
			} catch (CancellationException e) {
				throw e;
			} catch (FeedParserException e) {
				pw.warn("Failed parsing feed", e);
				mNotices.add("Failed parsing feed of "+sub+": "+e);
			} catch (IOException e) {
				pw.warn("Failed parsing feed", e);
				mNotices.add("Failed connect feed of "+sub+": "+e);
			} catch (Exception e) {
				pw.warn("Failed parsing feed", e);
				mNotices.add("Fatal while parsing feed of "+sub+": "+e);
			}
		}

		updateCountArticles();
		loadPage(mOffset);
	}

	private void downloadArticles() {
		checkCancellation();

		for(Subscription sub : mSubscriptions.keySet()) {
			checkCancellation();
			Feeds feeds = mSubscriptions.get(sub);

			if (feeds == null) {
				Log.d(TAG, "Feeds list is null "+sub);
				continue;
			}

			pw.debug("Start download and parse full articles of " + sub);
			for (FeedItem feedItem : feeds) {
				checkCancellation();
				try {
					final SelectArticleWorkflow wkflow = new SelectArticleWorkflow(mContext, feedItem, mArticleTimeToLive, true, null);

					if (mArticlesLoader == null) { //synchronize mode, download article in the same thread as tag loading
						pw.resetStopwatch();

						mCurrentLoadArticleWorkflow = wkflow;
						mCurrentLoadArticleWorkflow.run();

						pw.t("Upsert excerpt "+feedItem);
					}
					else {
						pw.trace("Add to article loader queue "+feedItem);
						mPendingLoadArticleWorkflow.add(wkflow);
						mArticlesLoader.execute(wkflow);
					}
				}
				catch (CancellationException e) {
					throw e;
				} catch (Exception e) {
					pw.warn("Failed updating article", e);
					mNotices.add("Failed updating article "+feedItem+": "+e);
				}
			}
			pw.debug("Finished download full articles of " + sub);

			pw.resetStopwatch();

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

			DaoMaster daoMaster = mRefData.createWritableDaoMaster();
			try {
				DaoSession daoSession = daoMaster.newSession();
				daoSession.getSubscriptionDao().update(sub);
			}
			finally {
				daoMaster.getDatabase().close();
			}
			pw.d("Update " + sub + " in database");
		}
	}

	private boolean isExpiry(Subscription sub) {
		if (mSubscriptionsTimeToLive == null || sub.getLastUpdate() == null) {
			return true;
		}
		return  new Duration(new DateTime(sub.getLastUpdate()), DateTime.now()).isLongerThan(mSubscriptionsTimeToLive);
	}

	private void buildArticlesQuery() {
		checkCancellation();
		if (mSelectArticleQueryBuilder == null) {
			pw.resetStopwatch();

			mSelectArticleQueryBuilder = buildArticlesQuery(mSubscriptions.keySet().toArray(new Subscription[mSubscriptions.size()]));

			pw.d("Build article query from " + mSubscriptions.size() + " subscriptions");
		}
	}

	/**
	 * Query to find all article related to a subscription list. Use to find all article of a given Tag
	 */
	public QueryBuilder<Article> buildArticlesQuery(Subscription[] subscriptions) {
		if (subscriptions == null || subscriptions.length==0) {
			return null;
		}

		QueryBuilder<Article> queryBuilder = mDaoSessionReadonly.getArticleDao().queryBuilder();

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

//	/**
//	 * Find subscriptions by tag: Select * from Subscription where tag like "%|tag|%"
//	 * @param tag
//	 */
//	public List<Subscription> getSubscriptions(String tag) {
//		if (Strings.isNullOrEmpty(tag)) {
//			QueryBuilder<Subscription> qb = daoSessionReadonly.getSubscriptionDao().queryBuilder();
//			qb.where(
//					SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
//					qb.or(SubscriptionDao.Properties.Tags.isNull(), SubscriptionDao.Properties.Tags.eq("")));
//
//			checkAccessDiskOnMainThread();
//			return qb.list();
//		}
//
//		checkAccessDiskOnMainThread();
//		return daoSessionReadonly.getSubscriptionDao().queryBuilder()
//				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
//						SubscriptionDao.Properties.Tags.like("%"+ TagUtils.getTechnicalTag(tag)+"%")).list();
//	}


	@Inject RefData mRefData;

	/**
	 * Find subscriptions by tag from subscriptions list cached in memory
	 */
	public List<Subscription> getSubscriptions(String tag) {
		List<Subscription> resu = new ArrayList<Subscription>();
		String normalizeTag = TagUtils.getTechnicalTag(tag);
		for (Subscription sub : mRefData.getActiveSubscriptions()) {
			if (sub.getTags().contains(normalizeTag)) {
				resu.add(sub);
			}
		}
		return resu;
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
		pw.debug("Cancelled");
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

	/*public int getOffset() {
		return mOffset;
	}*/

	/*public List<Article> getInMemoryCache() {
		return mArticles;
	}*/

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

//	/**
//	 * return true if the tag and all the articles is finished downloading
//	 */
//	public boolean isFinishedAll() {
//		if (mArticlesLoader == null || !mOnlineMode) { //sync mode or offline mode
//			return isFinished();
//		}
//		else {
//			if (mPendingLoadArticleWorkflow==null || mPendingLoadArticleWorkflow.size()==0) {
//				/*if (Constants.DEBUG) {
//					throw new IllegalStateException("Too early, this method can only be called after downloadArticles()");
//				}
//				else {
//					return false;
//				}*/
//				return false;
//			}
//			for (SelectArticleWorkflow wk : mPendingLoadArticleWorkflow) {
//				if (!wk.isFinished()) {
//					return false;
//				}
//			}
//			return true;
//		}
//	}

	public int countArticlesToDownload() {
		if (mPendingLoadArticleWorkflow==null || mPendingLoadArticleWorkflow.size()==0) {
//			if (Constants.DEBUG) {
//				throw new IllegalStateException("Too early, this method can only be called after downloadArticles()");
//			}
//			else {
//				return -1;
//			}
			return -1;
		}
		return mPendingLoadArticleWorkflow.size();
	}

	/**
	 * return end time of the last download article workflow task
	 */
	public Calendar getEndTimeAll() {
		if (mArticlesLoader == null || !mOnlineMode) { //sync mode or offline mode
			return getEndTime();
		}
		else {
			if (mPendingLoadArticleWorkflow==null || mPendingLoadArticleWorkflow.size()==0) {
				/*if (Constants.DEBUG) {
					throw new IllegalStateException("Too early, this method can only be called after downloadArticles()");
				}
				else {
					return null;
				}*/
				return null;
			}

			Calendar lastEndTime = null;
			for (SelectArticleWorkflow wk : mPendingLoadArticleWorkflow) {
				if (wk.getEndTime() == null) {
					return null;
				}
				if (lastEndTime == null || lastEndTime.before(wk.getEndTime())) {
					lastEndTime = wk.getEndTime();
				}
			}
			return lastEndTime;
		}
	}

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
			pw.warn("Access disk on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

//	@Override
//	public int compareTo(Object another) {
//		int c = super.compareTo(another);
//		if (c==0) {
//			return this.getMissionId().compareTo(((PrifoTask)another).getMissionId());
//		}
//		else {
//			return c;
//		}
//	}

//	@Override
//	public void onEnterQueue(PrifoQueue queue) {
//		Log.i(TAG, String.format("EnterQueue %s - %d/%d jobs (%d ms)", getMissionId(), queue.countActiveTasks(), queue.size(), mStopwatchQueue.elapsed(TimeUnit.MILLISECONDS)));
//	}
//
//	@Override
//	public void onDequeue(PrifoQueue queue) {
//		Log.i(TAG, String.format("DeQueue %s - %d/%d jobs (%d ms)", getMissionId(), queue.countActiveTasks(), queue.size(), mStopwatchQueue.elapsed(TimeUnit.MILLISECONDS)));
//	}

	public static interface SelectTagCallback {
		public void onFinishedLoadFromCache(SelectTagWorkflow sender, List<Article> articles, int count);
		public void onFinishedDownloadFeeds(SelectTagWorkflow sender, List<Article> articles, int count);
		public void onFinishedDownloadArticles(SelectTagWorkflow sender);
		public void done(SelectTagWorkflow sender, List<Article> articles, int count, List<String> notices, boolean isCancelled);
	}
}
