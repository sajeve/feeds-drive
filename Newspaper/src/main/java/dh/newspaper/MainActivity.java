package dh.newspaper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import dagger.Lazy;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingActivity;
import dh.newspaper.base.Injector;
import dh.newspaper.model.FakeDataProvider;
import dh.newspaper.view.CategoriesFragment;
import dh.newspaper.view.FeedsFragment;

import javax.inject.Inject;
import javax.inject.Singleton;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();

	@Inject
	Lazy<CategoriesFragment> mCategoriesFragment;

	@Inject
	Lazy<FeedsFragment> mFeedsFragment;

	private boolean mTwoPane = false;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		((Injector) getApplication()).inject(this);

		/*mCategoriesFragment = (CategoriesFragment) getFragmentManager()
				.findFragmentById(R.id.categories_drawer);
		mTitle = getTitle();*/

		// Set up the drawer.
		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mTwoPane = drawerLayout == null;
		if (!mTwoPane) {
			mCategoriesFragment.get().setUpAsNavigationDrawer(this, R.id.categories_drawer, drawerLayout);
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.fragment_feeds_container, mFeedsFragment.get())
					.commit();
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


	public void onEvent(CategoriesFragment.Event e) {
		try {
			if (!mTwoPane) {
	/*			FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.fragment_feeds_container, mFeedsFragment.get())
						.commit();
	*/		}
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "MainActivity FeedsFragment.Event.ON_FRAGMENT_ATTACHED", ex);
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
		if (!mCategoriesFragment.get().isDrawerOpen()) {
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
