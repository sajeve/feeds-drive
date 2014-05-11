package dh.newspaper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.etsy.android.grid.StaggeredGridView;
import de.greenrobot.event.EventBus;
import dh.newspaper.adapter.ArticlePreviewGridAdapter;
import dh.newspaper.base.InjectingActivity;
import dh.newspaper.base.InjectingFragment;
import dh.newspaper.event.EventOneArg;
import dh.newspaper.modules.ParserModule;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.RssItem;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends InjectingActivity {
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		//System.setProperty("http.useragent", "");
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

	/**
	 * Invoke by EventBus when {@link dh.newspaper.NavigationDrawerFragment} item selected
	 */
	public void onEvent(NavigationDrawerFragment.Event e) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(e.intArg + 1)).commit();
	}

	/**
	 * Invoke by EventBus when {@link dh.newspaper.MainActivity.PlaceholderFragment} attached
	 */
	public void onEvent(PlaceholderFragment.Event e) {

		switch (e.intArg) {
			case 1:
				mTitle = getString(R.string.title_section1);
				break;
			case 2:
				mTitle = getString(R.string.title_section2);
				break;
			case 3:
				mTitle = getString(R.string.title_section3);
				break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends InjectingFragment {
		static final String TAG = PlaceholderFragment.class.getName();
		@Inject ContentParser contentParser;

		ArticlePreviewGridAdapter gridViewAdapter;
		List<RssItem> data = new ArrayList<RssItem>();

		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);

			return fragment;
		}

		public PlaceholderFragment() {
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

			StaggeredGridView gridView = (StaggeredGridView)rootView.findViewById(R.id.articles_gridview);
			if (this.getActivity()!=null && gridView != null) {
				//gridViewAdapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, this.data);
				gridViewAdapter = new ArticlePreviewGridAdapter(this.getActivity(), R.layout.article_preview, data);
				gridView.setAdapter(gridViewAdapter);

				AsyncTask<String, Void, List<RssItem>> articlesDownloadTask = new AsyncTask<String, Void, List<RssItem>>() {
					@Override
					protected List<RssItem> doInBackground(String... params) {
						try {
							//URL feedUrl = new URL("http://vnexpress.net/rss/thoi-su.rss");
							String feedUrl = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml";
							return contentParser.parseRssUrl(feedUrl, "UTF-8");
						} catch (Exception ex) {
							Log.w(TAG, ex);
						}

						return null;
					}

					@Override
					protected void onPostExecute(java.util.List<RssItem> result) {
						try {
							PlaceholderFragment.this.data.clear();
                            if (result!=null) {
                                PlaceholderFragment.this.data.addAll(result);
                            }
							gridViewAdapter.notifyDataSetChanged();
						}
						catch (Exception ex) {
							Log.w(TAG, ex);
						}
					}
				};

				articlesDownloadTask.execute();
			}

			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			final int sessionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
			EventBus.getDefault().post(new Event() {{ intArg=sessionNumber; }});
		}

		@Override
		protected List<Object> getModules() {
			return new ArrayList<Object>() {{ add(new ParserModule()); }};
		}

		public class Event extends EventOneArg<PlaceholderFragment> {
			public Event() {
				super(PlaceholderFragment.this);
			}
			public Event(String subject) {
				super(PlaceholderFragment.this, subject);
			}
		}
	}

}
