package dh.newspaper.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import com.google.common.base.Stopwatch;
import de.greenrobot.event.EventBus;
import dh.newspaper.*;
import dh.newspaper.adapter.ArticlesGridAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.event.RefreshFeedsListEvent;
import dh.newspaper.model.generated.Article;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.tools.StrUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class FeedsFragment extends Fragment {
	static final String TAG = FeedsFragment.class.getName();

	private SwipeRefreshLayout mSwipeRefreshLayout;
	private GridView mGridView;
	private ArticlesGridAdapter mGridViewAdapter;
	private String mCurrentTag;

	/**
	 * use to trace the current pending event
	 */
	private UUID mEventFlowId;

	@Inject
	AppBundle mAppBundle;

	@Inject
	BackgroundTasksManager mBackgroundTasksManager;

	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_TAG = "tag";

	public FeedsFragment() {
		super();
		//setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		mSwipeRefreshLayout = (SwipeRefreshLayout)inflater.inflate(R.layout.fragment_feeds, container, false);
		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				loadTag(mCurrentTag);
			}
		});

		if (isAdded()) {
			final Activity currentActivity = this.getActivity();

			int numberOfColumns = (currentActivity instanceof DetailActivity) ? 1
					: getResources().getInteger(R.integer.grid_columns_count);

			mGridViewAdapter = new ArticlesGridAdapter(currentActivity, numberOfColumns);
			loadTag(mCurrentTag);

			//mGridView = (StaggeredGridView) rootView.findViewById(R.id.articles_gridview);
			mGridView = (GridView) mSwipeRefreshLayout.findViewById(R.id.articles_gridview);
			//mGridView.setColumnCount(numberOfColumns);
			mGridView.setNumColumns(numberOfColumns);
			//rootView.forceLayout();
			mGridView.setAdapter(mGridViewAdapter);
			mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					try {
						if (position == ListView.INVALID_POSITION || !FeedsFragment.this.isAdded()) {
							return;
						}

						Article article = (Article) parent.getItemAtPosition(position);
						if (article == null)
							article = (Article)mGridViewAdapter.getItem(position);

						if (article == null) {
							Log.w(TAG, "select null article");
							mGridViewAdapter.notifyDataSetChanged();
							return;
						}

						Log.d("DebugClick", "Clicked on "+article);

						final Activity currentActivity = FeedsFragment.this.getActivity();
						if (currentActivity instanceof MainActivity) {
							Intent detailIntent = new Intent(currentActivity, DetailActivity.class);
							//detailIntent.putExtra("article", article);
							startActivity(detailIntent);
						}

						mGridView.setItemChecked(position, true);
						EventBus.getDefault().post(new Event(Event.ON_ITEM_SELECTED, article));
					}
					catch (Exception ex) {
						Log.w(TAG, ex);
					}
				}
			});

//			FeedItemLoader feedItemLoader = new FeedItemLoader((DatabaseActivity)currentActivity);
//			ItemManager.Builder builder = new ItemManager.Builder(feedItemLoader);
//			builder.setPreloadItemsEnabled(true).setPreloadItemsCount(5);
//			builder.setThreadPoolSize(4);
//			ItemManager itemManager = builder.build();
//
//			mGridViewAdapter = new FeedsGridAdapter(currentActivity, mContentParser, numberOfColumns);
//			refreshContent();
//
//			mGridView = (AsyncGridView) rootView.findViewById(R.id.articles_gridview);
//			mGridView.setNumColumns(numberOfColumns);
//			mGridView.setAdapter(mGridViewAdapter);
//			mGridView.setOnItemClickListener(onArticleClickListener);
//			mGridView.setItemManager(itemManager);
		}

		return mSwipeRefreshLayout;
	}

	private boolean mFirstAttach = true;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// make sure it's the first time through; we don't want to re-inject a retained fragment that is going
		// through a detach/attach sequence.
		if (mFirstAttach) {
			((Injector) activity.getApplication()).inject(this);
			mFirstAttach = false;
		}

		if (activity instanceof MainActivity && getArguments()!=null) {
			final String currentTag = getArguments().getString(ARG_TAG);
			EventBus.getDefault().post(new Event(Event.ON_FRAGMENT_ATTACHED) {{
				stringArg = currentTag;
			}});
		}
		loadTag(mAppBundle.getCurrentTag());
	}

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		EventBus.getDefault().unregister(this);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(ARG_TAG, mCurrentTag);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		loadTag(savedInstanceState.getString(ARG_TAG));
	}

	@Override
	public void onDestroy() {
		if (mGridViewAdapter!=null && mGridViewAdapter.getData() != null) {
			mGridViewAdapter.getData().close();
		}
		super.onDestroy();
	}

	public void onEventMainThread(TagsFragment.Event event) {
		if (!isAdded()) {
			return;
		}
		try {
			loadTag(event.getTag());
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	Stopwatch swRfle = Stopwatch.createStarted();
	public void onEventMainThread(RefreshFeedsListEvent event) {
		if (!isAdded() || mGridViewAdapter==null) {
			return;
		}
		try {
			switch (event.getSubject()) {
				case Constants.SUBJECT_FEEDS_START_LOADING:
					if (Constants.DEBUG) {
						swRfle = Stopwatch.createStarted();
						Log.d("DebugClick", "FeedsFragment START " + event.getSender().getTag() + " - " + mEventFlowId);
					}

					mSwipeRefreshLayout.setRefreshing(true);
					mCurrentTag = event.getSender().getTag();

					//Save the identity of the current running workflow recognise future events coming from the same workflow
					mEventFlowId = event.getFlowId();

					return;
				case Constants.SUBJECT_FEEDS_REFRESH:
					if (mEventFlowId != event.getFlowId()) {
						//this event is fired by a sender which is no more concerning by this fragment -> do nothing
						return;
					}

					if (Constants.DEBUG) {
						Log.d("DebugClick", "FeedsFragment REFRESH (" + swRfle.elapsed(TimeUnit.MILLISECONDS) + " ms) " + event.getSender().getTag() + " - " + mEventFlowId);
						swRfle.reset().start();
					}

					mGridViewAdapter.setData(event.getArticles(), event.getCount());
					mGridViewAdapter.notifyDataSetChanged();
					return;
				case Constants.SUBJECT_FEEDS_DONE_LOADING:
					if (mEventFlowId != event.getFlowId()) {
						//this event is fired by a sender which is no more concerning by this fragment -> do nothing
						return;
					}

					if (Constants.DEBUG)
						Log.d("DebugClick", "FeedsFragment DONE ("+swRfle.elapsed(TimeUnit.MILLISECONDS)+" ms) "+event.getSender().getTag() + " - " + mEventFlowId);

					mGridViewAdapter.setData(event.getArticles(), event.getCount());
					mGridViewAdapter.notifyDataSetChanged();
					mSwipeRefreshLayout.setRefreshing(false);
					return;
			}

		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	private void loadTag(String tag) {
		mCurrentTag = tag;
		mBackgroundTasksManager.loadTag(tag);
	}

	public class Event extends BaseEventOneArg<FeedsFragment> {
		public static final String ON_FRAGMENT_ATTACHED = "OnFragmentAttached";
		public static final String ON_ITEM_SELECTED = "OnItemSelected";

		private Article mArticle;

		public Event(String subject) {
			super(FeedsFragment.this, subject);
		}
		public Event(String subject, Article article) {
			this(subject);
			mArticle = article;
		}
		public Article getArticle() {
			return mArticle;
		}
	}
}
