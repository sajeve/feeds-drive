package dh.newspaper;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.services.AlarmReceiver;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.services.FeedsDownloaderService;
import dh.newspaper.services.MainMenuHandler;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.view.ManageSubscriptionActivity;
import dh.newspaper.view.SubscriptionActivity;

import javax.inject.Inject;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();
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

	@Inject SharedPreferences mSharedPreferences;

	@Inject MainMenuHandler mMainMenuHandler;

	@Inject RefData mRefData;
	@Inject BackgroundTasksManager mBackgroundTasksManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Injector)getApplication()).inject(this);
		//mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		// Set up the drawer.
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		if (isSinglePane()) {
			// Read in the flag indicating whether or not the user has demonstrated awareness of the
			// drawer. See PREF_USER_LEARNED_DRAWER for details.
			mUserLearnedDrawer = mSharedPreferences.getBoolean(Constants.PREF_USER_LEARNED_DRAWER, false);
			setUpAsNavigationDrawer();
		}

		overridePendingTransition(R.anim.left_in, R.anim.right_out);

		/*else {
			View v = findViewById(R.id.fragment_feeds);
			Log.i(TAG, "fragment_feed.id = " + v.getMissionId());
		}*/

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
//		if (isDrawerOpen()) {
//			// Only show items in the action bar relevant to this screen
//			// if the drawer is not showing. Otherwise, let the drawer
//			// decide what to show in the action bar.
//			getMenuInflater().inflate(R.menu.main, menu);
//			restoreActionBar();
//			return true;
//		}
//		restoreActionBar();
//		return super.onCreateOptionsMenu(menu);



		getMenuInflater().inflate(R.menu.main, menu);

		//restore menu state
		MenuItem offlineItem = menu.findItem(R.id.action_offline);
		offlineItem.setChecked(!mRefData.getPreferenceOnlineMode());

		restoreActionBar();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//		int id = item.getItemId();
//		if (id == R.id.action_settings) {
//			return true;
//		}
//		if (id == R.id.action_offline) {
//			boolean newState = !item.isChecked();
//			item.setChecked(newState);
//			Log.d(TAG, "Switch to "+(newState ? "offline": "online")+" mode");
//			mSharedPreferences.edit().putBoolean(Constants.PREF_OFFLINE_KEY, newState).apply();
//			return true;
//		}

		if (mMainMenuHandler.onOptionsItemSelected(this, item)) {
			return true;
		}
		switch (item.getItemId()) {
			case R.id.action_subscribe:
				this.startActivity(new Intent(this, SubscriptionActivity.class));
				return true;
			case R.id.action_manage_subscription:
				this.startActivity(new Intent(this, ManageSubscriptionActivity.class));
				return true;
			case R.id.action_downloadAll: {
				/*long interval = mRefData.getPreferenceServiceInterval();
				AlarmReceiver.setupAlarm(getApplicationContext(), Constants.SERVICE_START_AT, interval);*/
				//force start service
				Intent downloadIntent = new Intent(this, FeedsDownloaderService.class);
				downloadIntent.putExtra(FeedsDownloaderService.CHECK_SERVICE_ENABLE, false);
				downloadIntent.putExtra(FeedsDownloaderService.CHECK_CHARGING_CONDITION, false);
				this.startService(downloadIntent);
				return true;
			}
			case R.id.action_cancel_download: {
				//force cancel service
				Intent downloadIntent = new Intent(this, FeedsDownloaderService.class);
				downloadIntent.putExtra(FeedsDownloaderService.CANCEL_SERVICE, true);
				this.startService(downloadIntent);

				//cancel current background activity
				mBackgroundTasksManager.cancelAllDownloading();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Invoke by EventBus when {@link dh.newspaper.view.FeedsFragment} attached
	 */
	public void onEventMainThread(FeedsFragment.Event e) {
		try {
			if (!FeedsFragment.Event.CHANGE_TITLE.equals(e.getSubject())) {
				return;
			}

			mTitle = e.stringArg;

			ActionBar actionBar = getActionBar();
			if (actionBar!=null)
				actionBar.setTitle(mTitle);

		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity FeedsFragment.Event.CHANGE_TITLE", ex);
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
//			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity FeedsFragment.Event.CHANGE_TITLE", ex);
//		}
//	}



	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	private final String STATE_TITLE = "title";

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequence(STATE_TITLE, mTitle);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mTitle = savedInstanceState.getCharSequence(STATE_TITLE);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.left_in, R.anim.right_out);
	}

	/**
	 * Set up the navigation drawer interactions.
	 */
	public void setUpAsNavigationDrawer() {
		mFragmentContainerView = findViewById(R.id.fragment_tags);

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
				if (!isSinglePane()) {
					return; //no sense for two-pane layout
				}

				invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				if (!isSinglePane()) {
					return; //no sense for two-pane layout
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
