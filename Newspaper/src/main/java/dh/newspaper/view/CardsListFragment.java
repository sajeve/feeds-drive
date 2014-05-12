package dh.newspaper.view;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import com.etsy.android.grid.StaggeredGridView;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.adapter.ArticlePreviewGridAdapter;
import dh.newspaper.base.InjectingFragment;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.modules.ParserModule;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.RssItem;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class CardsListFragment extends InjectingFragment {
	static final String TAG = CardsListFragment.class.getName();

	private StaggeredGridView mGridView;
	private ArticlePreviewGridAdapter mGridViewAdapter;

	@Inject
	ContentParser mContentParser;

	/**
	 * The fragment argument representing the section number for this
	 * fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Returns a new instance of this fragment for the given section number.
	 */
	public static CardsListFragment newInstance(int sectionNumber) {
		CardsListFragment fragment = new CardsListFragment();
		Bundle args = new Bundle();
		args.putInt(ARG_SECTION_NUMBER, sectionNumber);
		fragment.setArguments(args);

		return fragment;
	}

	public CardsListFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container,
				false);
		TextView textView = (TextView) rootView
				.findViewById(R.id.section_label);

		int sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
		textView.setText(Integer.toString(sectionNumber));

		if (this.getActivity() != null) {

			mGridViewAdapter = new ArticlePreviewGridAdapter(this.getActivity(), mContentParser);
			mGridViewAdapter.fetchAddress("http://vnexpress.net/rss/thoi-su.rss");

			mGridView = (StaggeredGridView) rootView.findViewById(R.id.articles_gridview);
			mGridView.setAdapter(mGridViewAdapter);

			mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					try {
						final RssItem itemData = (RssItem) parent.getItemAtPosition(position);

						//display reader
						FragmentManager fragmentManager = getFragmentManager();
						fragmentManager
								.beginTransaction()
								.replace(R.id.container,
										ReaderFragment.newInstance(itemData)).commit();

						//EventBus.getDefault().post(new Event("GridView.ItemClick") {{rssItem = itemData; }});
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

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		final int sessionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
		EventBus.getDefault().post(new Event("onAttach") {{
			intArg = sessionNumber;
		}});
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

	public void onEventMainThread(ArticlePreviewGridAdapter.Event e) {
		//update the Adapter data + refresh GUI
		mGridViewAdapter.clear();
		mGridViewAdapter.addAll(e.data);
	}

	public class Event extends BaseEventOneArg<CardsListFragment> {
		public Event() {
			super(CardsListFragment.this);
		}
		public Event(String subject) {
			super(CardsListFragment.this, subject);
		}
	}
}
