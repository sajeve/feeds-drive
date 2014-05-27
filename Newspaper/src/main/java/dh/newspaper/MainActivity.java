package dh.newspaper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import dagger.Lazy;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.DatabaseActivity;
import dh.newspaper.base.Injector;
import dh.newspaper.model.FakeDataProvider;
import dh.newspaper.view.TagsFragment;
import dh.newspaper.view.FeedsFragment;

import javax.inject.Inject;

public class MainActivity extends DatabaseActivity {
	private static final String TAG = MainActivity.class.getName();

	@Inject
	Lazy<TagsFragment> mCategoriesFragment;

	@Inject
	Lazy<FeedsFragment> mFeedsFragment;

	/**
	 * Helper component that ties the action bar to the navigation drawer.
	 */
	private ActionBarDrawerToggle mDrawerToggle;

	private View mFragmentContainerView;

	/**
	 * True if user has learned about the drawer
	 */
	private boolean mUserLearnedDrawer;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	private DrawerLayout mDrawerLayout;

	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Injector) getApplication()).inject(this);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Set up the drawer.
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		if (isSinglePane()) {
			// Read in the flag indicating whether or not the user has demonstrated awareness of the
			// drawer. See PREF_USER_LEARNED_DRAWER for details.
			mUserLearnedDrawer = mSharedPreferences.getBoolean(Constants.PREF_USER_LEARNED_DRAWER, false);

			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.fragment_feeds_container, mFeedsFragment.get())
					.commit();

			setUpAsNavigationDrawer();
		}

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

//	/**
//	 * Invoke by EventBus when {@link dh.newspaper.view.CategoriesFragment} item selected
//	 */
//	public void onEvent(CategoriesFragment.Event e) {
//		try {
//			if (mFeedsFragment == null) {
//				return;
//			}
//
//			// update the main content by replacing fragments
//			FragmentManager fragmentManager = getFragmentManager();
//
//			/*//clear backstack before
//			for(int i = 0; i < fragmentManager.getBackStackEntryCount(); ++i) {
//				fragmentManager.popBackStack();
//			}*/
//
//			fragmentManager
//					.beginTransaction()
//					.replace(R.id.container, mFeedsFragment)
//					.commit();
//		} catch (Exception ex) {
//			Log.w(TAG, ex);
//			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity on NavigationDrawerFragment.Event", ex);
//		}
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (isDrawerOpen()) {
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
	 * Invoke by EventBus when {@link dh.newspaper.view.FeedsFragment} attached
	 */
	public void onEvent(FeedsFragment.Event e) {
		try {
			if (!FeedsFragment.Event.ON_FRAGMENT_ATTACHED.equals(e.getSubject())) {
				return;
			}
			mTitle = FakeDataProvider.getCategories()[e.intArg-1];
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity FeedsFragment.Event.ON_FRAGMENT_ATTACHED", ex);
		}
	}

//	public void onEvent(CategoriesFragment.Event e) {
//		try {
//			if (!mTwoPane) {
//	/*			FragmentManager fragmentManager = getFragmentManager();
//				fragmentManager.beginTransaction()
//						.replace(R.id.fragment_feeds_container, mFeedsFragment.get())
//						.commit();
//	*/		}
//		}catch (Exception ex) {
//			Log.w(TAG, ex);
//			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity FeedsFragment.Event.ON_FRAGMENT_ATTACHED", ex);
//		}
//	}



	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	/**
	 * Set up the navigation drawer interactions.
	 */
	public void setUpAsNavigationDrawer() {
		mFragmentContainerView = findViewById(R.id.tags_drawer);

		// set a custom shadow that overlays the main content when the drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// set up the drawer's list view with items and click listener
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the navigation drawer and the action bar app icon.
		mDrawerToggle = new ActionBarDrawerToggle(
				this,                    /* host Activity */
				mDrawerLayout,                    /* DrawerLayout object */
				R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
				R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
				R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
		) {
			@Override
			public void onDrawerClosed(View drawerView) {
				super.onDrawerClosed(drawerView);
				if (!mCategoriesFragment.get().isAdded()) {
					return;
				}

				invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if (!mCategoriesFragment.get().isAdded()) {
					return;
				}

				if (!mUserLearnedDrawer) {
					// The user manually opened the drawer; store this flag to prevent auto-showing
					// the navigation drawer automatically in the future.
					mUserLearnedDrawer = true;
					mSharedPreferences.edit().putBoolean(Constants.PREF_USER_LEARNED_DRAWER, true).apply();
				}

				invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}
		};

		// If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
		// per the navigation drawer design guidelines.
		if (!mUserLearnedDrawer) {
			mDrawerLayout.openDrawer(mFragmentContainerView);
		}

		// Defer code dependent on restoration of previous instance state.
		mDrawerLayout.post(new Runnable() {
			@Override
			public void run() {
				mDrawerToggle.syncState();
			}
		});

		mDrawerLayout.setDrawerListener(mDrawerToggle);
	}

	public boolean isDrawerOpen() {
		return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
	}

	public ActionBarDrawerToggle getDrawerToggle() {
		return mDrawerToggle;
	}

	public void closeDrawer() {
		if (mDrawerLayout != null) {
			mDrawerLayout.closeDrawer(mFragmentContainerView);
		}
	}

	private boolean isTwoPane() {
		return mDrawerLayout == null;
	}
	private boolean isSinglePane() {
		return mDrawerLayout != null;
	}
}
