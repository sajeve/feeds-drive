package dh.newspaper.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import de.psdev.slf4j.android.logger.AndroidLoggerAdapter;
import de.psdev.slf4j.android.logger.LogLevel;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.RefreshArticleEvent;
import dh.newspaper.event.RefreshFeedsListEvent;
import dh.newspaper.event.RefreshTagsListEvent;
import dh.newspaper.model.generated.Article;
import dh.newspaper.workflow.SearchFeedsTask;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.PrifoExecutors;
import dh.newspaper.workflow.SelectArticleWorkflow;
import dh.newspaper.workflow.SelectTagWorkflow;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
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
	private ExecutorService mArticlesLoader = PrifoExecutors.newCachedThreadExecutor(1, Constants.THREAD_ARTICLES_LOADER);
	//private ExecutorService mArticlesLoader = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private PriorityExecutor mArticlesLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	private SelectTagWorkflow mSelectTagWorkflow;
	private ExecutorService mSelectTagLoader = PrifoExecutors.newCachedThreadExecutor(1, Integer.MAX_VALUE);
	//private ExecutorService mSelectTagLoader = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private ExecutorService mSelectTagLoader = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LifoBlockingDeque<Runnable>());

	private SelectArticleWorkflow mSelectArticleWorkflow;
	private ExecutorService mSelectArticleLoader = PrifoExecutors.newCachedThreadExecutor(8, Integer.MAX_VALUE);
	//private ExecutorService mSelectArticleLoader = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private ExecutorService mSelectArticleLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	@Inject RefData mRefData;

	@Inject SharedPreferences mSharedPreferences;

	private Handler mMainThreadHandler;

	public BackgroundTasksManager(Context context) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mContext = context;
		mMainThreadHandler = new Handler();
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
				EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_START_LOADING));
				try {
					mRefData.getLruDiscCache(); //setupLruDiscCache
					mRefData.loadTags();
				}
				finally {
					EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_REFRESH));
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				mRefData.initImageLoader();
				if (!Constants.DEBUG) {
					//load the first tag
					if (mRefData.getTags().size() > 0) {
						loadTag(mRefData.getTags().first());
					}
				}
			}
		};

		mInitThread.execute();
	}

	/**
	 * Load tags and notify GUI to display it from {@link dh.newspaper.cache.RefData#getTags()}
	 */
	public void loadTagsList() {
		if (mTagsListLoader != null) {
			mTagsListLoader.shutdownNow();
		}

		mTagsListLoader = Executors.newSingleThreadExecutor();

		if (mRefData.isTagsListReadyInMemory()) {
			EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_REFRESH));
		}

		mTagsListLoader.execute(new Runnable() {
			@Override
			public void run() {
				try {
					EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_START_LOADING));
					mRefData.loadTags();
					EventBus.getDefault().post(new RefreshTagsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_TAGS_REFRESH));
				} catch (Exception ex) {
					Log.w(TAG, ex);
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
					if (mSelectTagWorkflow != null) {
						//the same tag is loading
						if (StrUtils.equalsIgnoreCases(mSelectTagWorkflow.getTag(), tag) && mSelectTagWorkflow.isRunning()) {
							Log.d(TAG, String.format("%s is running (priority=%d%s)",
									mSelectTagWorkflow,
									mSelectTagWorkflow.getPriority(),
									mSelectTagWorkflow.isFocused() ? " focused" : ""));
						}
						mSelectTagWorkflow.cancel();
					}

					final boolean onlineMode = !mSharedPreferences.getBoolean(Constants.PREF_OFFLINE, Constants.PREF_OFFLINE_DEFAULT);

					mSelectTagWorkflow = new SelectTagWorkflow(mContext, tag, Constants.SUBSCRIPTION_TTL, Constants.ARTICLE_TTL, onlineMode, Constants.ARTICLES_PER_PAGE, mArticlesLoader, new SelectTagWorkflow.SelectTagCallback() {
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
					mSelectTagLoader.execute(mSelectTagWorkflow);
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
					if (mSelectArticleWorkflow != null) {
						//the same article is loading
						if (mSelectArticleWorkflow.getFeedItem().getUri().equals(article.getArticleUrl()) && mSelectArticleWorkflow.isRunning()) {
							Log.d(TAG, String.format("%s is running (priority=%d%s)",
									mSelectArticleWorkflow,
									mSelectArticleWorkflow.getPriority(),
									mSelectArticleWorkflow.isFocused() ? " focused" : ""));
						}
						mSelectArticleWorkflow.cancel();
					}

					final boolean onlineMode = !mSharedPreferences.getBoolean(Constants.PREF_OFFLINE, Constants.PREF_OFFLINE_DEFAULT);

					mSelectArticleWorkflow = new SelectArticleWorkflow(mContext, article, Constants.ARTICLE_TTL, onlineMode, new SelectArticleWorkflow.SelectArticleCallback() {
						@Override
						public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, sender.getArticleUrl()));
						}
						@Override
						public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article) {

						}
						@Override
						public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, sender.getArticleUrl()));
						}
						@Override
						public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_DONE_LOADING, sender.getArticleUrl()));
						}
					});
					mSelectArticleWorkflow.setFocus(true);

					EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_START_LOADING, mSelectArticleWorkflow.getArticleUrl()));
					mSelectArticleLoader.execute(mSelectArticleWorkflow);
				} catch (Exception ex) {
					Log.w(TAG, ex);
					EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_DONE_LOADING, mSelectArticleWorkflow.getArticleUrl()));
				}
			}
		};

		mMainThreadHandler.postDelayed(lastLoadArticleCall, Constants.EVENT_DELAYED);
	}


	private SearchFeedsTask activeSearchFeedsTask;
	public void searchFeedsSources(final String query) throws UnsupportedEncodingException {
		if (Strings.isNullOrEmpty(query)) {
			return;
		}
		//cancel last task
		if (activeSearchFeedsTask != null) {
			activeSearchFeedsTask.cancel();
		}
		activeSearchFeedsTask = new SearchFeedsTask(mContext, query);
		activeSearchFeedsTask.setFocus(true);
		mSelectArticleLoader.execute(activeSearchFeedsTask);
	}

	public SelectTagWorkflow getActiveSelectTagWorkflow() {
		return mSelectTagWorkflow;
	}
	public SelectArticleWorkflow getActiveSelectArticleWorkflow() {
		return mSelectArticleWorkflow;
	}
	public SearchFeedsTask getActiveSearchFeedsTask() {
		return activeSearchFeedsTask;
	}

	@Override
	public void close() throws IOException {
		if (mTagsListLoader != null) {
			mTagsListLoader.shutdownNow();
		}
		if (mSelectTagLoader != null) {
			mSelectTagLoader.shutdownNow();
		}
		if (mArticlesLoader != null) {
			mArticlesLoader.shutdownNow();
		}
		if (mInitThread != null) {
			mInitThread.cancel(false);
		}
	}
}
