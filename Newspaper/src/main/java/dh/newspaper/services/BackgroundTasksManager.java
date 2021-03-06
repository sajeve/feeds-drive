package dh.newspaper.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.webkit.URLUtil;
import com.google.common.base.Strings;
import com.nostra13.universalimageloader.core.ImageLoader;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.RefreshArticleEvent;
import dh.newspaper.event.RefreshFeedsListEvent;
import dh.newspaper.event.RefreshTagsListEvent;
import dh.newspaper.event.SaveSubscriptionEvent;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.workflow.SaveSubscriptionWorkflow;
import dh.newspaper.workflow.SearchFeedsWorkflow;
import dh.tool.thread.prifo.*;
import dh.newspaper.workflow.SelectArticleWorkflow;
import dh.newspaper.workflow.SelectTagWorkflow;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hiep on 4/06/2014.
 */
public class BackgroundTasksManager implements Closeable {
	private static final String TAG = BackgroundTasksManager.class.getName();

	//private final ThreadFactory ThreadFactoryHigherPriority = new PriorityThreadFactory(Thread.NORM_PRIORITY+1);
	private Context mContext;

	private ExecutorService mTagsListLoader;
	private PrifoExecutor mArticlesLoader;

	private SelectTagWorkflow mSelectTagWorkflow;
	//private ExecutorService mSelectTagLoader = PrifoExecutorFactory.newPrifoExecutor(2, Integer.MAX_VALUE);

	private SelectArticleWorkflow mSelectArticleWorkflow;
	private PrifoExecutor mMainPrifoExecutor;

	@Inject RefData mRefData;
	@Inject SharedPreferences mSharedPreferences;
	@Inject ConnectivityManager mConnectivityManager;
	@Inject DatabaseHelper mDatabaseHelper;

	private Handler mMainThreadHandler;

	public BackgroundTasksManager(Context context) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mContext = context;
		mMainThreadHandler = new Handler();
		mArticlesLoader = mRefData.createArticlesLoader("ArticleLoader");
		mMainPrifoExecutor = RefData.createMainExecutor();
	}

	private boolean isInitWorkflowRun = false;
	private AsyncTask mInitThread;
	public void runInitialisationWorkflow() {
		if (isInitWorkflowRun) {
			throw new IllegalStateException("Initialisation workflow is already run");
		}

		isInitWorkflowRun = true;

		mInitThread = new AsyncTask<Object, Object, Boolean>() {
			@Override
			protected Boolean doInBackground(Object[] params) {
				//install database
				if (!Constants.SINGLE_DATABASE_CONNECTION) {
					SQLiteDatabase pingConnection = mDatabaseHelper.getWritableDatabase();
					pingConnection.close();
				}

				EventBus.getDefault().post(new RefreshTagsListEvent(Constants.SUBJECT_TAGS_START_LOADING));
				try {
					mRefData.getLruDiscCache(); //setupLruDiscCache
					mRefData.loadSubscriptionAndTags();
				}
				finally {
					EventBus.getDefault().post(new RefreshTagsListEvent(Constants.SUBJECT_TAGS_END_LOADING));
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				mRefData.initImageLoader();
				if (Constants.LOAD_FIRST_TAG_ON_START) {
					//load the first tag
					if (mRefData.getActiveTags().size() > 0) {
						loadTag(mRefData.getActiveTags().first());
					}
				}
			}
		};

		mInitThread.execute();
	}

	/**
	 * Load tags and notify GUI to display it from {@link dh.newspaper.cache.RefData#getActiveTags()}
	 */
	public void loadTagsList() {
		if (mTagsListLoader != null) {
			mTagsListLoader.shutdown();
		}

		mTagsListLoader = Executors.newSingleThreadExecutor();

		/*if (mRefData.isTagsListReadyInMemory()) {
			EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_END_LOADING));
		}*/

		mTagsListLoader.execute(new Runnable() {
			@Override
			public void run() {
				try {
					EventBus.getDefault().post(new RefreshTagsListEvent(Constants.SUBJECT_TAGS_START_LOADING));
					mRefData.loadSubscriptionAndTags();
				} catch (Exception ex) {
					Log.w(TAG, ex);
				}
				finally {
					EventBus.getDefault().post(new RefreshTagsListEvent(Constants.SUBJECT_TAGS_END_LOADING));
				}
			}
		});
	}

	private Runnable lastLoadTagCall;
	public void loadTag(final String tag) {
		if (tag == null) return;

		if (lastLoadTagCall!=null) {
			mMainThreadHandler.removeCallbacks(lastLoadTagCall); //we received new call, so remove the last one
		}

		lastLoadTagCall = new Runnable() {
			@Override
			public void run() {
				try {
//					if (mSelectTagWorkflow != null) {
//						//the same tag is loading
//						if (StrUtils.equalsIgnoreCases(mSelectTagWorkflow.getTag(), tag) && mSelectTagWorkflow.isRunning()) {
//							Log.d(TAG, String.format("%s is running (priority=%d%s)",
//									mSelectTagWorkflow,
//									mSelectTagWorkflow.getPriority(),
//									mSelectTagWorkflow.isFocused() ? " focused" : ""));
//						}
//						mSelectTagWorkflow.cancel();
//					}

					mRefData.updateArticleLoaderPoolSize(mArticlesLoader);

					mSelectTagWorkflow = new SelectTagWorkflow(mContext, tag, Constants.SUBSCRIPTION_TTL_MIN, Constants.INFINITE_DURATION /*Constants.ARTICLE_TTL_MIN*/, isOnline(), Constants.ARTICLES_PER_PAGE, mArticlesLoader, new SelectTagWorkflow.SelectTagCallback() {
						@Override
						public void onFinishedLoadFromCache(SelectTagWorkflow sender, List<Article> articles, int count) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_REFRESH, sender.getTag()));
						}

						@Override
						public void onFinishedDownloadFeeds(SelectTagWorkflow sender, List<Article> articles, int count) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_REFRESH, sender.getTag()));
						}

						@Override
						public void onFinishedDownloadArticles(SelectTagWorkflow sender) {
						}

						@Override
						public void done(SelectTagWorkflow sender, List<Article> articles, int count, List<String> notices, boolean isCancelled) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_DONE_LOADING, sender.getTag()));
						}
					});
					mSelectTagWorkflow.setFocus(true);
					EventBus.getDefault().post(new RefreshFeedsListEvent(mSelectTagWorkflow, Constants.SUBJECT_FEEDS_START_LOADING, mSelectTagWorkflow.getTag()));
					//mSelectTagLoader.execute(mSelectTagWorkflow);
					executeUniqueOnMainExecutor(mSelectTagWorkflow);
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
					EventBus.getDefault().post(new RefreshFeedsListEvent(mSelectTagWorkflow, Constants.SUBJECT_FEEDS_DONE_LOADING, mSelectTagWorkflow.getTag()));
				}
			}
		};

		mMainThreadHandler.postDelayed(lastLoadTagCall, Constants.EVENT_DELAYED);
	}

	private Runnable lastLoadArticleCall;

	/**
	 * Use post delayed technique to execute only the last call, if there are too much consecutive call
	 * @param article
	 */
	public void loadArticle(final Article article) {
		if (article == null) {
			return;
		}

		if (lastLoadArticleCall!=null) {
			mMainThreadHandler.removeCallbacks(lastLoadArticleCall); //we received new call for loadArticle, so remove the last one
		}

		lastLoadArticleCall = new Runnable() {
			@Override
			public void run() {
				try
				{
					/*if (mSelectArticleWorkflow != null) {
						//the same article is loading
						if (mSelectArticleWorkflow.getFeedItem().getUri().equals(article.getArticleUrl()) && mSelectArticleWorkflow.isRunning()) {
							Log.d(TAG, String.format("%s is running (priority=%d%s)",
									mSelectArticleWorkflow,
									mSelectArticleWorkflow.getPriority(),
									mSelectArticleWorkflow.isFocused() ? " focused" : ""));
						}
						mSelectArticleWorkflow.cancel();
					}*/

					mSelectArticleWorkflow = new SelectArticleWorkflow(mContext, article, Constants.ARTICLE_TTL_MIN, isOnline(), new SelectArticleWorkflow.SelectArticleCallback() {
						@Override
						public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article) {
							RefreshArticleEvent event = new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, sender.getArticleUrl());
							Log.v(TAG, "Post "+event);
							EventBus.getDefault().post(event);
						}
						@Override
						public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article, String fullContent) {
							RefreshArticleEvent event = new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, sender.getArticleUrl(), fullContent);
							Log.v(TAG, "Post "+event);
							EventBus.getDefault().post(event);
						}
						@Override
						public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew) {
							RefreshArticleEvent event = new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, sender.getArticleUrl());
							Log.v(TAG, "Post "+event);
							EventBus.getDefault().post(event);
						}
						@Override
						public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled) {
							RefreshArticleEvent event = new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_DONE_LOADING, sender.getArticleUrl());
							Log.v(TAG, "Post "+event);
							EventBus.getDefault().post(event);
						}
					});
					mSelectArticleWorkflow.setFocus(true);

					EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_START_LOADING, mSelectArticleWorkflow.getArticleUrl()));
					executeUniqueOnMainExecutor(mSelectArticleWorkflow);
				} catch (Exception ex) {
					Log.w(TAG, ex);
					EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_DONE_LOADING, mSelectArticleWorkflow.getArticleUrl()));
				}
			}
		};

		mMainThreadHandler.postDelayed(lastLoadArticleCall, Constants.EVENT_DELAYED);
	}

	private PrifoExecutor mServiceTagsLoader;
	private PrifoExecutor mServiceArticlesLoader;
	public void cancelServiceDownloadAll() {
		if (mServiceTagsLoader !=null) {
			mServiceTagsLoader.shutdown();
			mServiceTagsLoader.cancelAll();
		}
		if (mServiceArticlesLoader !=null) {
			mServiceArticlesLoader.shutdown();
			mServiceArticlesLoader.cancelAll();
		}
	}
	private void initServiceLoaders() {
		cancelServiceDownloadAll();
		mServiceTagsLoader = PrifoExecutorFactory.newPrifoExecutor("ServiceTagsLoader");
		mServiceArticlesLoader = mRefData.createArticlesLoader("ServiceArticlesLoader");
	}
	public void runServiceDownloadAll() {
		if (mRefData.getActiveTags() == null) {
			Log.w(TAG, "Cannot run downloading service, initialization did not finished");
			return;
		}
		//cancel old tasks
		initServiceLoaders();
		//Duration serviceInterval = new Duration(mRefData.getPreferenceServiceInterval());
		Duration serviceInterval = new Duration(Long.MAX_VALUE); //never download article already downloaded

		//selectTagWorkflowList = new ArrayList<SelectTagWorkflow>();
		for (String tag : mRefData.getActiveTags()) {
			SelectTagWorkflow selectTagWorkflow = new SelectTagWorkflow(mContext, tag,
					Constants.SUBSCRIPTION_TTL_MIN, serviceInterval, true, Constants.ARTICLES_PER_PAGE,
					mServiceArticlesLoader, null);
			mServiceTagsLoader.execute(selectTagWorkflow);
		}
	}

	private boolean isOnline() {
		return
			RefData.getPreferenceOnlineMode(mSharedPreferences)
			&& NetworkUtils.networkConditionMatched(mConnectivityManager, mSharedPreferences);
	}

	private SearchFeedsWorkflow activeSearchFeedsWorkflow;
	public void searchFeedsSources(final String query) throws UnsupportedEncodingException {
		if (Strings.isNullOrEmpty(query)) {
			return;
		}
		/*//cancel last task
		if (activeSearchFeedsTask != null) {
			activeSearchFeedsTask.cancel();
		}*/
		activeSearchFeedsWorkflow = new SearchFeedsWorkflow(mContext, query);
		activeSearchFeedsWorkflow.setFocus(true);
		executeUniqueOnMainExecutor(activeSearchFeedsWorkflow);
	}

	/**
	 * Save or delete
	 */
	private OncePrifoTask currentSaveDeleteSubscriptionTask;
	public void saveSubscription(final SearchFeedsResult.ResponseData.Entry feedsSource, final Set<String> tags) {
		if (feedsSource==null || !URLUtil.isValidUrl(feedsSource.getUrl())) {
			if (Constants.DEBUG) {
				throw new InvalidParameterException("Feeds Source invalid");
			}
			return; //nothing to do
		}
		currentSaveDeleteSubscriptionTask = new SaveSubscriptionWorkflow(mContext, feedsSource, tags);
		executeUniqueOnMainExecutor(currentSaveDeleteSubscriptionTask);
	}

	//@Inject DaoSession daoSession;
	public void deleteSubscription(final Subscription sub) {
		currentSaveDeleteSubscriptionTask = new OncePrifoTask() {
			@Override
			public void perform() {
				try {
					sendProgressMessage("Deleting subscription.."); //TODO: translate

					mDatabaseHelper.operate(new DatabaseHelper.DatabaseOperation() {
						@Override
						public void doOperate(DaoSession daoSession) {
							daoSession.getSubscriptionDao().delete(sub);
						}
					});

					sendProgressMessage("Reloading cache.."); //TODO: translate
					mRefData.loadSubscriptionAndTags();
					sendDone();
				}catch (Exception ex) {
					sendError("Failed deleting subscription: " + ex); //TODO: translate
					Log.w(TAG, ex);
				}
			}

			@Override
			public String getMissionId() {
				return sub.getFeedsUrl();
			}

			private void sendProgressMessage(String message) {
				SaveSubscriptionEvent saveSubscriptionState = new SaveSubscriptionEvent(
						Constants.SUBJECT_SAVE_SUBSCRIPTION_PROGRESS_MESSAGE,
						sub.getFeedsUrl(),
						message);
				EventBus.getDefault().post(saveSubscriptionState);
			}
			private void sendError(String message) {
				SaveSubscriptionEvent saveSubscriptionState = new SaveSubscriptionEvent(
						Constants.SUBJECT_SAVE_SUBSCRIPTION_ERROR,
						sub.getFeedsUrl(),
						message);
				EventBus.getDefault().post(saveSubscriptionState);
			}
			private void sendDone() {
				SaveSubscriptionEvent saveSubscriptionState = new SaveSubscriptionEvent(
						Constants.SUBJECT_SAVE_SUBSCRIPTION_DONE,
						sub.getFeedsUrl());
				EventBus.getDefault().post(saveSubscriptionState);
			}
		};
		executeUniqueOnMainExecutor(currentSaveDeleteSubscriptionTask);
	}

	public SelectTagWorkflow getActiveSelectTagWorkflow() {
		return mSelectTagWorkflow;
	}
	public SelectArticleWorkflow getActiveSelectArticleWorkflow() {
		return mSelectArticleWorkflow;
	}
	public SearchFeedsWorkflow getActiveSearchFeedsWorkflow() {
		return activeSearchFeedsWorkflow;
	}
	public OncePrifoTask getCurrentSaveDeleteSubscriptionTask() {
		return currentSaveDeleteSubscriptionTask;
	}

	/**
	 * submit new task on main executor, cancel all current task.
	 * Use to execute only unique task, which should be done on GUI thread
	 */
	private void executeUniqueOnMainExecutor(PrifoTask task) {
		cancelMainExecutor();
		mMainPrifoExecutor.execute(task);

		//mMainPrifoExecutor.executeUnique(task);
	}

	/**
	 * shutdown the mainExecutor and replace it by other thread pool
	 */
	private void cancelMainExecutor() {
		if (mMainPrifoExecutor!=null) {
			mMainPrifoExecutor.shutdown();
			mMainPrifoExecutor.cancelAll();
		}
		mMainPrifoExecutor = RefData.createMainExecutor();
	}

	public void cancelAllDownloading() {
		//mMainPrifoExecutor.cancelAll();
		cancelServiceDownloadAll();
		cancelMainExecutor();
		mArticlesLoader.cancelAll();
		{
			PrifoTask t = getActiveSelectTagWorkflow();
			if (t != null){
				t.cancel();
			}
		}
		{
			PrifoTask t = getActiveSelectArticleWorkflow();
			if (t != null){
				t.cancel();
			}
		}
		{
			PrifoTask t = getActiveSearchFeedsWorkflow();
			if (t != null){
				t.cancel();
			}
		}
		ImageLoader.getInstance().stop();
	}

	@Override
	public void close() throws IOException {
		if (mTagsListLoader != null) {
			mTagsListLoader.shutdownNow();
		}
		/*if (mSelectTagLoader != null) {
			mSelectTagLoader.shutdownNow();
		}*/
		if (mArticlesLoader != null) {
			mArticlesLoader.shutdown();
		}
		if (mInitThread != null) {
			mInitThread.cancel(false);
		}
		if (mServiceArticlesLoader != null) {
			mServiceArticlesLoader.shutdownNow();
		}
		if (mServiceTagsLoader != null) {
			mServiceTagsLoader.shutdownNow();
		}
		mDatabaseHelper.close();
	}

}
