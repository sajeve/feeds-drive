package dh.newspaper.services;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import de.greenrobot.dao.query.LazyList;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.RefreshArticleEvent;
import dh.newspaper.event.RefreshFeedsListEvent;
import dh.newspaper.event.RefreshTagsListEvent;
import dh.newspaper.model.generated.Article;
import dh.newspaper.tools.*;
import dh.newspaper.workflow.SelectArticleWorkflow;
import dh.newspaper.workflow.SelectTagWorkflow;
import roboguice.util.temp.Strings;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by hiep on 4/06/2014.
 */
public class BackgroundTasksManager implements Closeable {
	private static final String TAG = BackgroundTasksManager.class.getName();

	//private final ThreadFactory ThreadFactoryHigherPriority = new PriorityThreadFactory(Thread.NORM_PRIORITY+1);
	private Context mContext;

	private ExecutorService mTagsListLoader;
	private ExecutorService mArticlesLoader = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private PriorityExecutor mArticlesLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	private SelectTagWorkflow mSelectTagWorkflow;
	private ExecutorService mActiveTagLoader = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private ExecutorService mActiveTagLoader = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LifoBlockingDeque<Runnable>());

	private SelectArticleWorkflow mSelectArticleWorkflow;
	private ExecutorService mActiveArticleLoader = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new BumpBlockingQueue());
	//private ExecutorService mActiveArticleLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	@Inject
	RefData mRefData;

	private Handler mMainThreadHandler;

	public BackgroundTasksManager(Context context) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mContext = context;
		mMainThreadHandler = new Handler();
	}

	/**
	 * Load tags and notify GUI to display it from {@link dh.newspaper.cache.RefData#getTags()}
	 */
	public void loadTagsList() {
		if (mTagsListLoader != null) {
			mTagsListLoader.shutdownNow();
		}

		mTagsListLoader = Executors.newSingleThreadExecutor();

		if (mRefData.isTagsAvailableInMemory()) {
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

		if (lastLoadTagCall!=null) {
			mMainThreadHandler.removeCallbacks(lastLoadTagCall); //we received new call, so remove the last one
		}

		lastLoadTagCall = new Runnable() {
			@Override
			public void run() {
				try {
					if (mSelectTagWorkflow != null) {
						//the same tag is loading
						if (Strings.equalsIgnoreCase(mSelectTagWorkflow.getTag(), tag) && mSelectTagWorkflow.isRunning()) {
							Log.i(TAG, mSelectTagWorkflow+" is running. No need to run it again");
							return;
						}

						try {
							mSelectTagWorkflow.close();
						} catch (IOException e) {
							Log.w(TAG, e);
						}
					}

					final UUID flowId = UUID.randomUUID();
					mSelectTagWorkflow = new SelectTagWorkflow(mContext, tag, Constants.SUBSCRIPTION_TTL, Constants.ARTICLE_TTL, mArticlesLoader, new SelectTagWorkflow.SelectTagCallback() {
						@Override
						public void onFinishedLoadFromCache(SelectTagWorkflow sender, LazyList<Article> articles, int count) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_REFRESH, flowId, articles, count));
						}

						@Override
						public void onFinishedDownloadFeeds(SelectTagWorkflow sender, LazyList<Article> articles, int count) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_REFRESH, flowId, articles, count));
						}

						@Override
						public void onFinishedDownloadArticles(SelectTagWorkflow sender) {
						}

						@Override
						public void done(SelectTagWorkflow sender, LazyList<Article> articles, int count, List<String> notices, boolean isCancelled) {
							EventBus.getDefault().post(new RefreshFeedsListEvent(sender, Constants.SUBJECT_FEEDS_DONE_LOADING, flowId, articles, count));
						}
					});
					mSelectTagWorkflow.setActive(true);

					EventBus.getDefault().post(new RefreshFeedsListEvent(mSelectTagWorkflow, Constants.SUBJECT_FEEDS_START_LOADING, flowId, null, 0));
					mActiveTagLoader.execute(mSelectTagWorkflow);

					/*mActiveTagLoader.execute(new Runnable() {
						@Override
						public void run() {
							try {
								EventBus.getDefault().post(new RefreshFeedsListEvent(mSelectTagWorkflow, Constants.SUBJECT_FEEDS_START_LOADING, null, 0));
								mSelectTagWorkflow.run();
							} catch (Exception ex) {
								Log.w(TAG, ex);
								EventBus.getDefault().post(new RefreshFeedsListEvent(mSelectTagWorkflow, Constants.SUBJECT_FEEDS_DONE_LOADING, null, 0));
							}
						}
					});*/
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
		};

		mMainThreadHandler.postDelayed(lastLoadTagCall, Constants.EVENT_DELAYED);
	}


	private Runnable lastLoadArticleCall;

	/**
	 * Use post delayed technique to exeute only the last call, if there are too much consecutive call
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
							Log.i(TAG, mSelectArticleWorkflow+" is running. No need to run it again");
							return;
						}
						mSelectArticleWorkflow.cancel();
					}

					final UUID flowId = UUID.randomUUID();
					mSelectArticleWorkflow = new SelectArticleWorkflow(mContext, article, Constants.ARTICLE_TTL, true, new SelectArticleWorkflow.SelectArticleCallback() {
						@Override
						public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, flowId));
						}
						@Override
						public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article) {

						}
						@Override
						public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_REFRESH, flowId));
						}
						@Override
						public void done(SelectArticleWorkflow sender, Article article, boolean isCancelled) {
							EventBus.getDefault().post(new RefreshArticleEvent(sender, Constants.SUBJECT_ARTICLE_DONE_LOADING, flowId));
						}
					});
					mSelectArticleWorkflow.setActive(true);

					EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_START_LOADING, flowId));
					mActiveArticleLoader.execute(mSelectArticleWorkflow);

					/*mActiveArticleLoader.execute(new Runnable() {
						@Override
						public void run() {
							try {
								EventBus.getDefault().post(new RefreshArticleEvent(mSelectArticleWorkflow, Constants.SUBJECT_ARTICLE_START_LOADING));
								mSelectArticleWorkflow.run();
							} catch (Exception ex) {
								Log.w(TAG, ex);
							}
						}
					});*/
				}
				catch (Exception ex)
				{
					Log.w(TAG, ex);
				}
			}
		};

		mMainThreadHandler.postDelayed(lastLoadArticleCall, Constants.EVENT_DELAYED);
	}

	@Override
	public void close() throws IOException {
		if (mSelectTagWorkflow != null) {
			try {
				mSelectTagWorkflow.close();
			} catch (IOException e) {
				Log.w(TAG, e);
			}
		}

		if (mTagsListLoader != null) {
			mTagsListLoader.shutdownNow();
		}
		if (mActiveTagLoader != null) {
			mActiveTagLoader.shutdownNow();
		}
		if (mArticlesLoader != null) {
			mArticlesLoader.shutdownNow();
		}
		/*if (mActiveArticleLoader != null) {
			mActiveArticleLoader.shutdownNow();
		}*/
	}
}
