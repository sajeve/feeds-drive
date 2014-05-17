package dh.newspaper.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.etsy.android.grid.StaggeredGridView;
import de.greenrobot.event.EventBus;
import dh.newspaper.DetailActivity;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.adapter.FeedsGridAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.model.FakeDataProvider;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.model.FeedItem;

import javax.inject.Inject;

/**
 * A placeholder fragment containing a simple view.
 */
public class FeedsFragment extends Fragment {
	static final String TAG = FeedsFragment.class.getName();

	private StaggeredGridView mGridView;
	private FeedsGridAdapter mGridViewAdapter;
	private int mCategoryId = 0;

	@Inject
	AppBundle mAppBundle;

	@Inject
	ContentParser mContentParser;

	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	public FeedsFragment() {
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_feeds, container,
				false);
		TextView textView = (TextView) rootView
				.findViewById(R.id.section_label);

		final Activity currentActivity = this.getActivity();

		if (isAdded()) {
			if (currentActivity instanceof MainActivity && getArguments()!=null) {
				int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
				textView.setText(Integer.toString(sectionNumber));
			}

			mGridViewAdapter = new FeedsGridAdapter(this.getActivity(), mContentParser);
			refreshContent();

			mGridView = (StaggeredGridView) rootView.findViewById(R.id.articles_gridview);
			mGridView.setAdapter(mGridViewAdapter);

			mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					try {
						if (position == ListView.INVALID_POSITION) {
							return;
						}

						final FeedItem feedItem = (FeedItem) parent.getItemAtPosition(position);

						if (currentActivity instanceof MainActivity) {
							Intent detailIntent = new Intent(currentActivity, DetailActivity.class);
							//detailIntent.putExtra("FeedItem", feedItem);
							startActivity(detailIntent);
						}

						mGridView.setItemChecked(position, true);
						EventBus.getDefault().post(new Event(Event.ON_ITEM_SELECTED, feedItem));
					}
					catch (Exception ex) {
						Log.w(TAG, ex);
					}
				}
			});


		}
		//URL feedUrl = new URL("http://vnexpress.net/rss/thoi-su.rss");
		//String feedUrl = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml

		return rootView;
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
			final int sessionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
			EventBus.getDefault().post(new Event(Event.ON_FRAGMENT_ATTACHED) {{
				intArg = sessionNumber;
			}});
		}
		mCategoryId = mAppBundle.getCurrentCategoryId();
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
		outState.putInt(ARG_SECTION_NUMBER, mCategoryId);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		mCategoryId = savedInstanceState.getInt(ARG_SECTION_NUMBER);
		refreshContent();
	}

	public void onEventMainThread(FeedsGridAdapter.Event e) {
		try {
			//update the Adapter data + refresh GUI
			mGridViewAdapter.clear();
			mGridViewAdapter.addAll(e.data);
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "OnFeedsDownloadFinished", ex);
		}
	}

	public void onEventMainThread(CategoriesFragment.Event e) {
		try {
			loadCategory(e.getCategoryId());
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "OnLoadCategory", ex);
		}
	}

	/**
	 * refresh the categoryPreview base on the current mCategoryId
	 */
	private void refreshContent() {
		if (mGridViewAdapter != null) {
			mGridViewAdapter.fetchAddress(FakeDataProvider.getCategorySource(mCategoryId));
		}
	}
	private void loadCategory(int categoryId) {
		mCategoryId = categoryId;
		if (mGridViewAdapter != null) {
			mGridViewAdapter.fetchAddress(FakeDataProvider.getCategorySource(mCategoryId));
		}
	}

	public class Event extends BaseEventOneArg<FeedsFragment> {
		public static final String ON_FRAGMENT_ATTACHED = "OnFragmentAttached";
		public static final String ON_ITEM_SELECTED = "OnItemSelected";

		private FeedItem mRssItem;

		public Event(String subject) {
			super(FeedsFragment.this, subject);
		}
		public Event(String subject, FeedItem rssItem) {
			this(subject);
			mRssItem = rssItem;
		}
		public FeedItem getFeedItem() {
			return mRssItem;
		}
	}
}
