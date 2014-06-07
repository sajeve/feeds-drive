package dh.newspaper.workflow;

import android.content.Context;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.greenrobot.dao.query.LazyList;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.tools.PriorityExecutor;
import dh.newspaper.tools.StrUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 3/06/2014.
 */
public class SelectTagWorkflow implements Closeable {
	private static final String TAG = SelectTagWorkflow.class.getName();

	@Inject DaoSession mDaoSession;
	@Inject ContentParser mContentParser;
	//@Inject MessageDigest mMessageDigest;
	//@Inject RefData refData;

	private final String mTag;
	private final Duration mSubscriptionsTimeToLive;
	private final Duration mArticleTimeToLive;
	private final PriorityExecutor mArticlesLoader;
	private final SelectTagCallback mCallback;
	private final Context mContext;

	private LazyList<Article> mArticles;
	private int mCountArticles;

	private HashMap<Subscription, Feeds> mSubscriptions;
	private QueryBuilder<Article> mSelectArticleQueryBuilder;

	private SelectArticleWorkflow mCurrentLoadArticleWorkflow;
	private List<SelectArticleWorkflow> mPendingLoadArticleWorkflow = new ArrayList<>();

	private List<String> mNotices = new ArrayList<>();

	private volatile boolean mCanceled = false;
	private Stopwatch mStopwatch;

	private volatile boolean used = false;
	private volatile boolean mRunning = true;

	public SelectTagWorkflow(Context context, String tag, Duration subscriptionsTimeToLive, Duration articleTimeToLive, PriorityExecutor articlesLoader, SelectTagCallback callback) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);

		mTag = tag;
		mSubscriptionsTimeToLive = subscriptionsTimeToLive;
		mArticleTimeToLive = articleTimeToLive;
		mContext = context;
		mArticlesLoader = articlesLoader;
		mCallback = callback;
	}

	/**
	 * Start the workflow
	 */
	public void run() throws WorkflowException {
		if (isCanceled()) {
			return;
		}
		if (used) {
			throw new IllegalStateException("Workflow is used");
		}
		used = true;
		mRunning = true;

		logSimple("Start workflow");
		Stopwatch genericStopWatch = Stopwatch.createStarted();
		mStopwatch = Stopwatch.createStarted();
		try {
			lazyLoadArticlesFromCache();
			if (mCallback != null && !isCanceled()) {
				resetStopwatch();
				mCallback.onFinishedLoadFromCache(this, mArticles, mCountArticles);
				log("Callback onFinishedLoadFromCache()");
			}

			downloadFeeds();
			if (mCallback != null && !isCanceled()) {
				resetStopwatch();
				mCallback.onFinishedDownloadFeeds(this, mArticles, mCountArticles);
				log("Callback onFinishedDownloadFeeds()");
			}

			downloadArticles();
			if (mCallback != null && !isCanceled()) {
				resetStopwatch();
				mCallback.onFinishedDownloadArticles(this);
				log("Callback onFinishedDownloadArticles()");
			}
		}
		finally {
			if (mCallback!=null) {
				resetStopwatch();
				mCallback.done(this, mArticles, mCountArticles, mNotices, isCanceled());
				log("Callback done()");
			}
			mRunning = false;
			logInfo("Workflow complete ("+genericStopWatch.elapsed(TimeUnit.MILLISECONDS)+" ms)");
		}
	}

	private void lazyLoadArticlesFromCache() throws WorkflowException {
		if (isCanceled()) {
			return;
		}
		logSimple("Load articles from cache");

		if (mSubscriptions == null || mSubscriptions.isEmpty()) {
			resetStopwatch();

			mSubscriptions = new HashMap<>();

			for (Subscription sub : getSubscriptions(mTag)) {
				if (sub!=null) {
					mSubscriptions.put(sub, null);
				}
				else {
					Log.w(TAG, "GreenDAO return null object");
				}
			}

			log("Found " + mSubscriptions.size() + " active subscriptions");
		}

		updateResult();
	}

	private void updateResult() {
		buildArticlesQuery();
		if (mSelectArticleQueryBuilder != null) {
			resetStopwatch();
			if (mArticles!=null) {
				mArticles.close();
				log("Close old result");
			}

			mArticles = mSelectArticleQueryBuilder.listLazy();
			log("Load articles to a lazy list");

			if (isCanceled()) {
				return;
			}

			mCountArticles = (int)mSelectArticleQueryBuilder.count();
			log("Count articles in lazy list = "+mCountArticles);
		}
	}

	private void downloadFeeds() {
		if (isCanceled()) {
			return;
		}
		for(Subscription sub : getSubscriptions(mTag)) {
			if (isCanceled()) {
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
				Feeds feeds = mContentParser.parseFeeds(sub.getFeedsUrl(), encoding);
				log("Download and parse feeds of "+sub);

				mSubscriptions.put(sub, feeds);

				//upsert article to database with excerpt
				for (FeedItem feedItem : feeds) {
					if (isCanceled()) {
						return;
					}
					try {
						resetStopwatch();

						mCurrentLoadArticleWorkflow = new SelectArticleWorkflow(mContext, feedItem, mArticleTimeToLive, false, null);
						mCurrentLoadArticleWorkflow.run();

						log("Download full content of "+feedItem);
					}
					catch (Exception e) {
						Log.w(TAG, e);
						mNotices.add("Failed updating article excerpt "+feedItem+": "+e.getMessage());
					}
				}
			} catch (FeedParserException e) {
				Log.w(TAG, e);
				mNotices.add("Failed parsing feed of "+sub+": "+e.getMessage());
			} catch (IOException e) {
				Log.w(TAG, e);
				mNotices.add("Failed connect feed of "+sub+": "+e.getMessage());
			} catch (Exception e) {
				Log.w(TAG, e);
				mNotices.add("Fatal while parsing feed of "+sub+": "+e.getMessage());
			}
		}

		if (mCountArticles == 0) {
			updateResult();
		}
	}

	private void downloadArticles() {
		if (isCanceled()) {
			return;
		}

		for(Subscription sub : mSubscriptions.keySet()) {
			if (isCanceled()) {
				return;
			}
			Feeds feeds = mSubscriptions.get(sub);

			if (feeds == null) {
				Log.d(TAG, "Feeds list is null "+sub);
				continue;
			}

			logSimple("Start download and parse full articles of " + sub);
			for (FeedItem feedItem : feeds) {
				if (isCanceled()) {
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
						mArticlesLoader.execute(new Runnable() {
							@Override
							public void run() {
								try {
									wkflow.run();
								}
								catch (Exception e) {
									Log.w(TAG, e);
									mNotices.add("Failed updating article "+wkflow.getFeedItem()+": "+e.getMessage());
								}
							}
						}, PriorityExecutor.MEDIUM);
					}
				}
				catch (Exception e) {
					Log.w(TAG, e);
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

			updateResult();
		}
	}

	private boolean isExpiry(Subscription sub) {
		if (mSubscriptionsTimeToLive == null || sub.getLastUpdate() == null) {
			return true;
		}
		return  new Duration(new DateTime(sub.getLastUpdate()), DateTime.now()).isLongerThan(mSubscriptionsTimeToLive);
	}

	private void buildArticlesQuery() {
		if (isCanceled()) {
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
			return qb.list();
		}

		return mDaoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
						SubscriptionDao.Properties.Tags.like("%"+getTechnicalTag(tag)+"%")).list();
	}

	private String getTechnicalTag(String tag) {
		return "|"+StrUtils.normalizeUpper(tag)+"|";
	}

	public boolean isCanceled() {
		return mCanceled || Thread.interrupted();
	}

	public synchronized void cancel() {
		mCanceled = true;
		if (mCurrentLoadArticleWorkflow != null) {
			mCurrentLoadArticleWorkflow.cancel();
		}
		if (mPendingLoadArticleWorkflow != null) {
			for (SelectArticleWorkflow saw : mPendingLoadArticleWorkflow) {
				saw.cancel();
			}
		}
	}

	@Override
	public void close() throws IOException {
		cancel();
		if (mArticles != null) {
			mArticles.close();
		}
	}

	public LazyList<Article> getResult() {
		return mArticles;
	}
	public int getResultSize() {
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

	private void logInfo(String message) {
		Log.i(TAG, message + " - " + mTag);
	}
	private void logSimple(String message) {
		Log.d(TAG, message + " - " + mTag);
	}
	private void log(String message) {
		Log.d(TAG, message + " ("+mStopwatch.elapsed(TimeUnit.MILLISECONDS)+" ms) - " + mTag);
		resetStopwatch();
	}
	private void resetStopwatch() {
		mStopwatch.reset().start();
	}


	public static interface SelectTagCallback {
		public void onFinishedLoadFromCache(SelectTagWorkflow sender, LazyList<Article> articles, int count);
		public void onFinishedDownloadFeeds(SelectTagWorkflow sender, LazyList<Article> articles, int count);
		public void onFinishedDownloadArticles(SelectTagWorkflow sender);
		public void done(SelectTagWorkflow sender, LazyList<Article> articles, int count, List<String> notices, boolean isCancelled);
	}
}
