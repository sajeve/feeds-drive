package dh.newspaper;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingActivity;
import dh.newspaper.view.CategoryPreviewFragment;
import dh.newspaper.view.NavigationDrawerFragment;

import javax.inject.Inject;

public class MainActivity extends InjectingActivity {
	private static final String TAG = MainActivity.class.getName();

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	@Inject CategoryPreviewFragment mCategoryPreviewFragment;

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
	 * Invoke by EventBus when {@link dh.newspaper.view.NavigationDrawerFragment} item selected
	 */
	public void onEvent(NavigationDrawerFragment.Event e) {
		try {
			if (mCategoryPreviewFragment == null) {
				return;
			}

			// update the main content by replacing fragments
			FragmentManager fragmentManager = getFragmentManager();

			/*//clear backstack before
			for(int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
				fragmentManager.popBackStack();
			}*/

			fragmentManager
					.beginTransaction()
					.replace(R.id.container, mCategoryPreviewFragment)
					.commit();
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity on NavigationDrawerFragment.Event", ex);
		}
	}

	/**
	 * Invoke by EventBus when {@link dh.newspaper.view.CategoryPreviewFragment} attached
	 */
	public void onEvent(CategoryPreviewFragment.Event e) {
		try {
			if (!CategoryPreviewFragment.Event.ON_FRAGMENT_ATTACHED.equals(e.getSubject())) {
				return;
			}
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
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity on CategoryPreviewFragment.Event", ex);
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

}
