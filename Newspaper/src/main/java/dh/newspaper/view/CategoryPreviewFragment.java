package dh.newspaper.view;

import android.annotation.TargetApi;
import android.app.Activity;
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
import dh.newspaper.CategoryPreviewActivity;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.adapter.ArticlePreviewGridAdapter;
import dh.newspaper.base.InjectingFragment;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.modules.ParserModule;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.RssItem;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class CategoryPreviewFragment extends InjectingFragment {
	static final String TAG = CategoryPreviewFragment.class.getName();

	private StaggeredGridView mGridView;
	private ArticlePreviewGridAdapter mGridViewAdapter;
	private int mCategoryId = -1;

	@Inject
	ContentParser mContentParser;

	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

//	/**
//	 * Returns a new instance of this fragment for the given section number.
//	 */
//	public static CategoryPreviewFragment newInstance(int sectionNumber) {
//		CategoryPreviewFragment fragment = new CategoryPreviewFragment();
//		Bundle args = new Bundle();
//		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
//		fragment.setArguments(args);
//
//		return fragment;
//	}

	@Inject
	public CategoryPreviewFragment() {
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_category_preview, container,
				false);
		TextView textView = (TextView) rootView
				.findViewById(R.id.section_label);

		final Activity currentActivity = this.getActivity();

		if (currentActivity != null) {
			if (currentActivity instanceof MainActivity && getArguments()!=null) {
				int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
				textView.setText(Integer.toString(sectionNumber));
			}

			mGridViewAdapter = new ArticlePreviewGridAdapter(this.getActivity(), mContentParser);
			mGridViewAdapter.fetchAddress("http://vnexpress.net/rss/thoi-su.rss");

			mGridView = (StaggeredGridView) rootView.findViewById(R.id.articles_gridview);
			mGridView.setAdapter(mGridViewAdapter);

			mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					try {
						if (position == ListView.INVALID_POSITION) {
							return;
						}

						final RssItem rssItem = (RssItem) parent.getItemAtPosition(position);

						if (currentActivity instanceof MainActivity) {
							Intent detailIntent = new Intent(currentActivity, CategoryPreviewActivity.class);
							detailIntent.putExtra("RssItem", rssItem);
							startActivity(detailIntent);
						}
						/*else if (currentActivity instanceof CategoryPreviewActivity) {
						}
						else {
							throw new IllegalStateException("CategoryFragment should be attached to MainActivity or CategoryPreviewActivity");
						}*/

						//display reader
						/*
						String stackName = mGridViewAdapter.getSourceAddress();
						FragmentManager fragmentManager = getFragmentManager();
						fragmentManager
								.beginTransaction()
								.replace(R.id.container,
										ReaderFragment.newInstance(rssItem))
								.addToBackStack(stackName)
								.commit();*/


						mGridView.setItemChecked(position, true);
						EventBus.getDefault().post(new Event(Event.ON_ITEM_SELECTED, rssItem));
					}
					catch (Exception ex) {
						Log.w(TAG, ex);
					}
				}
			});

			refreshCategoryPreview();
		}
		//URL feedUrl = new URL("http://vnexpress.net/rss/thoi-su.rss");
		//String feedUrl = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		if (activity instanceof MainActivity && getArguments()!=null) {
			final int sessionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
			EventBus.getDefault().post(new Event(Event.ON_FRAGMENT_ATTACHED) {{
				intArg = sessionNumber;
			}});
		}
	}

	@Override
	protected List<Object> getModules() {
		return new ArrayList<Object>() {{
			add(new ParserModule());
		}};
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
		refreshCategoryPreview();
	}

	public void onEventMainThread(ArticlePreviewGridAdapter.Event e) {
		try {
			//update the Adapter data + refresh GUI
			mGridViewAdapter.clear();
			mGridViewAdapter.addAll(e.data);
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "CategoryPreviewFragment on ArticlePreviewGridAdapter.Event", ex);
		}
	}

	public void onEventMainThread(NavigationDrawerFragment.Event e) {
		try {
			setAndRefreshCategoryPreview(e.getCategoryId());
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "CategoryPreviewFragment on NavigationDrawerFragment.Event", ex);
		}
	}

	/**
	 * refresh the categoryPreview base on the current mCategoryId
	 */
	private void refreshCategoryPreview() {
		if (mGridViewAdapter != null) {
			mGridViewAdapter.fetchAddress(getCategorySource(mCategoryId));
		}
	}
	private void setAndRefreshCategoryPreview(int categoryId) {
		mCategoryId = categoryId;
		if (mGridViewAdapter != null) {
			mGridViewAdapter.fetchAddress(getCategorySource(mCategoryId));
		}
	}

	private String getCategorySource(int categoryId) {
		switch (categoryId) {
			case 0: return "http://vnexpress.net/rss/tin-moi-nhat.rss";
			case 1: return "http://www.nytimes.com/services/xml/rss/nyt/AsiaPacific.xml";
			case 2: return "http://www.huffingtonpost.com/tag/asian-americans/feed";
			default: return null;
		}
	}

	public class Event extends BaseEventOneArg<CategoryPreviewFragment> {
		public static final String ON_FRAGMENT_ATTACHED = "OnFragmentAttached";
		public static final String ON_ITEM_SELECTED = "OnItemSelected";

		private RssItem mRssItem;

		public Event(String subject) {
			super(CategoryPreviewFragment.this, subject);
		}
		public Event(String subject, RssItem rssItem) {
			this(subject);
			mRssItem = rssItem;
		}
		public RssItem getRssItem() {
			return mRssItem;
		}
	}
}
