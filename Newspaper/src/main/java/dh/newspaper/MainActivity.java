package dh.newspaper;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import dh.newspaper.parser.ContentExtractor;
import dh.newspaper.parser.RssItem;

public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks {

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
		//mTitle = ContentExtractor.sayHello();

		// Set up the drawer.

		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		//System.setProperty("http.useragent", "");
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
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
	public static class PlaceholderFragment extends Fragment {
		static final String TAG = PlaceholderFragment.class.getName();
		ArrayAdapter<String> gridViewAdapter;
		ArrayList<String> data = new ArrayList<String>();

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
				gridViewAdapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_1, this.data);
				gridView.setAdapter(gridViewAdapter);

				AsyncTask<String, Void, ArrayList<String>> articlesDownloadTask = new AsyncTask<String, Void, ArrayList<String>>() {
					@Override
					protected ArrayList<String> doInBackground(String... params) {
						try {
							ArrayList<String> resu = new ArrayList<String>();

							//URL feedUrl = new URL("http://vnexpress.net/rss/thoi-su.rss");
							String feedUrl = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml";

							List<RssItem> feeds = ContentExtractor.parseRss(feedUrl, "UTF-8");

							for (RssItem t : feeds) {
								resu.add(t.getTitle());
							}

						/*	SyndFeedInput input = new SyndFeedInput();
							SyndFeed feed = input.build(new XmlReader(feedUrl));

							for (Object o : feed.getEntries()) {
								SyndEntry entry = (SyndEntry)o;
								//System.out.println(entry);
								resu.add(entry.getTitle());
//								System.out.println(entry.getTitle());
//								System.out.println(entry.getPublishedDate());
//								System.out.println(entry.getDescription().getType()); //if it is text/html
//								System.out.println(entry.getDescription().getValue()); //so get image from the first <img> tag here
//								System.out.println(entry.getUri());
//								System.out.println("-------------");
							}*/

							return resu;
							//System.out.println(feed);
						} catch (Exception ex) {
							Log.w(TAG, ex);
						}

						return null;
					}

					@Override
					protected void onPostExecute(java.util.ArrayList<String> result) {
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
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
	}

}
