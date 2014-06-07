package dh.newspaper.view;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.BaseEvent;
import dh.newspaper.event.RefreshTagsListEvent;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.tools.ArrayAdapterCompat;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class TagsFragment extends Fragment {
	static final String TAG = TagsFragment.class.getName();

	@Inject
	AppBundle mAppBundle;

	@Inject
	RefData mRefData;

	@Inject
	BackgroundTasksManager mBackgroundTasksManager;

	ArrayAdapterCompat<String> mDrawerListViewAdapter;

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ListView mDrawerListView;
	private SwipeRefreshLayout mSwipeRefreshLayout;

    private String mCurrentTag;

	@Inject
    public TagsFragment() {
		setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentTag = savedInstanceState.getString(STATE_SELECTED_POSITION);
        }
		else {
			mCurrentTag = mAppBundle.getCurrentTag();
		}

        // Select either the default item (0) or the last selected item.
        selectTag(mCurrentTag);

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
			mCurrentTag = mAppBundle.getCurrentTag();
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
		mSwipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(
				R.layout.fragment_tags, container, false);
        mDrawerListView = (ListView) mSwipeRefreshLayout.findViewById(R.id.list_tags);
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectTag(mDrawerListViewAdapter.getItem(position));
            }
        });

		mDrawerListViewAdapter = new ArrayAdapterCompat<String>(
				getActionBar().getThemedContext(),
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1,
				new ArrayList<String>());

		mDrawerListView.setAdapter(mDrawerListViewAdapter);

		int currentPosition = mDrawerListViewAdapter.getPosition(mCurrentTag);
        mDrawerListView.setItemChecked(currentPosition, true);

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mBackgroundTasksManager.loadTagsList();
			}
		});

		refreshTagsList();
        return mSwipeRefreshLayout;
    }

    private void selectTag(String tag) {
        mCurrentTag = tag;
        if (mDrawerListView != null) {
			int currentPosition = mDrawerListViewAdapter.getPosition(tag);
            mDrawerListView.setItemChecked(currentPosition, true);
        }

		closeDrawer();

		EventBus.getDefault().post(new Event(tag));
    }

	private void refreshTagsList() {
		if (mRefData.getTags() != null) {
			mDrawerListViewAdapter.clear();
			mDrawerListViewAdapter.addAll(mRefData.getTags());
			//mDrawerListViewAdapter.notifyDataSetChanged();
		}
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_POSITION, mCurrentTag);
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

	public void onEventMainThread(RefreshTagsListEvent event) {
		try {
			if (!isAdded()) {
				return;
			}
			switch (event.getSubject()) {
				case Constants.SUBJECT_TAGS_START_LOADING:
					mSwipeRefreshLayout.setRefreshing(true);
					return;
				case Constants.SUBJECT_TAGS_REFRESH:
					refreshTagsList();
					mSwipeRefreshLayout.setRefreshing(false);
					return;
			}
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
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

	public class Event extends BaseEvent<TagsFragment> {
		private String mTag;

		public Event(String tag) {
			super(TagsFragment.this);
			mTag = tag;
		}

		public String getTag() {
			return mTag;
		}

		/*public Event(String subject) {
			super(NavigationDrawerFragment.this, subject);
		}*/
	}
}
