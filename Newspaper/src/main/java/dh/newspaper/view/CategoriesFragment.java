package dh.newspaper.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.R;
import dh.newspaper.base.InjectingFragment;
import dh.newspaper.base.Injector;
import dh.newspaper.event.BaseEvent;
import dh.newspaper.model.FakeDataProvider;
import dh.newspaper.modules.AppBundle;

import javax.inject.Inject;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class CategoriesFragment extends Fragment {

	@Inject
	AppBundle mAppBundle;

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ListView mDrawerListView;

    private int mCurrentSelectedPosition = 0;

	@Inject
    public CategoriesFragment() {
		setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
        }
		else {
			mCurrentSelectedPosition = mAppBundle.getCurrentCategoryId();
		}

        // Select either the default item (0) or the last selected item.
        selectCategory(mCurrentSelectedPosition);
    }

	private boolean mFirstAttach = true;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		// make sure it's the first time through; we don't want to re-inject a retained fragment that is going
		// through a detach/attach sequence.
		if (mFirstAttach) {
			((Injector) activity.getApplication()).inject(this);
			mFirstAttach = false;
			mCurrentSelectedPosition = mAppBundle.getCurrentCategoryId();
		}
	}

    @Override
    public void onActivityCreated (Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDrawerListView = (ListView) inflater.inflate(
                R.layout.fragment_categories, container, false);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectCategory(position);
            }
        });
        mDrawerListView.setAdapter(new ArrayAdapter<String>(
                getActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1,
                android.R.id.text1,
				FakeDataProvider.getCategories()));
        mDrawerListView.setItemChecked(mCurrentSelectedPosition, true);
        return mDrawerListView;
    }

    private void selectCategory(final int position) {
        mCurrentSelectedPosition = position;
        if (mDrawerListView != null) {
            mDrawerListView.setItemChecked(position, true);
        }

		closeDrawer();

		EventBus.getDefault().post(new Event(position));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        getDrawerToggle().onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar. See also
        // showGlobalContextActionBar, which controls the top-left area of the action bar.
        if (isDrawerOpen()) {
            inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

	private boolean isDrawerOpen() {
		if (isAdded()) {
			return ((MainActivity)getActivity()).isDrawerOpen();
		}
		return false;
	}

	private ActionBarDrawerToggle getDrawerToggle() {
		if (isAdded()) {
			return ((MainActivity)getActivity()).getDrawerToggle();
		}
		return null;
	}

	private void closeDrawer() {
		if (isAdded()) {
			((MainActivity)getActivity()).closeDrawer();
		}
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		ActionBarDrawerToggle drawerToggle = getDrawerToggle();

		if (drawerToggle !=null && drawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		if (item.getItemId() == R.id.action_example) {
			Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to show the global app
     * 'context', rather than just what's in the current screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

	public class Event extends BaseEvent<CategoriesFragment> {
		private int mCategoryId;

		public Event(int categoryId) {
			super(CategoriesFragment.this);
			mCategoryId = categoryId;
		}

		public int getCategoryId() {
			return mCategoryId;
		}

		/*public Event(String subject) {
			super(NavigationDrawerFragment.this, subject);
		}*/
	}
}
