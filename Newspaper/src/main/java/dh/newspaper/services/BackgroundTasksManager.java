package dh.newspaper.services;

import android.content.Context;
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
import dh.newspaper.tools.LifoBlockingDeque;
import dh.newspaper.tools.PriorityExecutor;
import dh.newspaper.tools.PriorityThreadFactory;
import dh.newspaper.workflow.SelectArticleWorkflow;
import dh.newspaper.workflow.SelectTagWorkflow;
import roboguice.util.temp.Strings;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by hiep on 4/06/2014.
 */
public class BackgroundTasksManager implements Closeable {
	private static final String TAG = BackgroundTasksManager.class.getName();

	private final ThreadFactory ThreadFactoryHigherPriority = new PriorityThreadFactory(Thread.NORM_PRIORITY+1);
	private Context mContext;

	ExecutorService mTagsListLoader;
	PriorityExecutor mArticlesLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	SelectTagWorkflow mSelectTagWorkflow;
	ExecutorService mFeedsLoader = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LifoBlockingDeque<Runnable>());
	//ExecutorService mFeedsLoader;

	SelectArticleWorkflow mSelectArticleWorkflow;
	ExecutorService mActiveArticleLoader = PriorityExecutor.newCachedThreadPool(Constants.THREAD_POOL_SIZE);

	@Inject
	RefData mRefData;

	public BackgroundTasksManager(Context context) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		mContext = context;
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

	public void loadActiveTag(final String tag) {
		if (mSelectTagWorkflow != null) {
			//the same tag is loading

			if (Strings.equalsIgnoreCase(mSelectTagWorkflow.getTag(), tag) && mSelectTagWorkflow.isRunning()) {
				return;
			}

			try {
				mSelectTagWorkflow.close();
			} catch (IOException e) {
				Log.w(TAG, e);
			}
		}

		mSelectTagWorkflow = new SelectTagWorkflow(mContext, tag, Constants.SUBSCRIPTION_TTL, Constants.ARTICLE_TTL, mArticlesLoader, new SelectTagWorkflow.SelectTagCallback() {
			@Override
			public void onFinishedLoadFromCache(SelectTagWorkflow sender, LazyList<Article> articles, int count) {
				EventBus.getDefault().post(new RefreshFeedsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_FEEDS_REFRESH, articles, count));
			}

			@Override
			public void onFinishedDownloadFeeds(SelectTagWorkflow sender, LazyList<Article> articles, int count) {
				EventBus.getDefault().post(new RefreshFeedsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_FEEDS_REFRESH, articles, count));
			}

			@Override
			public void onFinishedDownloadArticles(SelectTagWorkflow sender) {
			}

			@Override
			public void done(SelectTagWorkflow sender, LazyList<Article> articles, int count, List<String> notices, boolean isCancelled) {
				EventBus.getDefault().post(new RefreshFeedsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_FEEDS_DONE_LOADING, articles, count));
			}
		});

		mFeedsLoader.execute(new Runnable() {
			@Override
			public void run() {
				try {
					EventBus.getDefault().post(new RefreshFeedsListEvent(BackgroundTasksManager.this, Constants.SUBJECT_FEEDS_START_LOADING, null, 0));
					mSelectTagWorkflow.run();
				} catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
		});
	}

	public void loadArticle(final Article article) {
		if (article == null) {
			return;
		}

		if (mSelectArticleWorkflow != null) {
			//the same article is loading
			if (mSelectArticleWorkflow.getFeedItem().getUri().equals(article.getArticleUrl()) && mSelectArticleWorkflow.isRunning()) {
				return;
			}
		}

		if (mActiveArticleLoader!=null) {
			mArticlesLoader.shutdownNow();
		}
		mActiveArticleLoader = Executors.newSingleThreadExecutor(ThreadFactoryHigherPriority);

		mSelectArticleWorkflow = new SelectArticleWorkflow(mContext, article, Constants.ARTICLE_TTL, true, new SelectArticleWorkflow.SelectArticleCallback() {
			@Override
			public void onFinishedCheckCache(SelectArticleWorkflow sender, Article article) {
				EventBus.getDefault().post(new RefreshArticleEvent(BackgroundTasksManager.this, Constants.SUBJECT_ARTICLE_REFRESH, sender));
			}
			@Override
			public void onFinishedDownloadContent(SelectArticleWorkflow sender, Article article) {

			}
			@Override
			public void onFinishedUpdateCache(SelectArticleWorkflow sender, Article article, boolean isInsertNew) {
				EventBus.getDefault().post(new RefreshArticleEvent(BackgroundTasksManager.this, Constants.SUBJECT_ARTICLE_REFRESH, sender));
			}
			@Override
			public void done(SelectArticleWorkflow sender, Article article) {
				EventBus.getDefault().post(new RefreshArticleEvent(BackgroundTasksManager.this, Constants.SUBJECT_ARTICLE_DONE_LOADING, sender));
			}
		});

		mActiveArticleLoader.execute(new Runnable() {
			@Override
			public void run() {
				try {
					EventBus.getDefault().post(new RefreshArticleEvent(BackgroundTasksManager.this, Constants.SUBJECT_ARTICLE_START_LOADING, null));
					mSelectArticleWorkflow.run();
				} catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
		});
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
		if (mFeedsLoader != null) {
			mFeedsLoader.shutdownNow();
		}
		if (mArticlesLoader != null) {
			mArticlesLoader.shutdownNow();
		}
		if (mActiveArticleLoader != null) {
			mActiveArticleLoader.shutdownNow();
		}
	}
}
