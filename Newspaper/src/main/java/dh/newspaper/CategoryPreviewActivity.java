package dh.newspaper;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingActivity;
import dh.newspaper.view.CategoryPreviewFragment;
import dh.newspaper.view.ReaderFragment;


/**
* An activity representing a list of Items. This activity
* has different presentations for handset and tablet-size devices. On
* handsets, the activity presents a list of items, which when touched,
* lead to a {@link ReaderActivity} representing
* item details. On tablets, the activity presents the list of items and
* item details side-by-side using two vertical panes.
* <p>
* The activity makes heavy use of fragments. The list of items is a
* {@link dh.newspaper.view.CategoryPreviewFragment} and the item details
* (if present) is a {@link dh.newspaper.view.ReaderFragment}.
* <p>
*/
public class CategoryPreviewActivity extends InjectingActivity {
	private static final String TAG = CategoryPreviewActivity.class.getName();

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_preview);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if (findViewById(R.id.reader_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
           /* ((CategoryFragment) getFragmentManager()
                    .findFragmentById(R.id.readeractivity_list))
                    .setActivateOnItemClick(true);*/
        }

        // TODO: If exposing deep links into your app, handle intents here.
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public void onEvent(CategoryPreviewFragment.Event e) {
		try {
			if (CategoryPreviewFragment.Event.ON_ITEM_SELECTED.equals(e.getSubject())) {
				return;
			}

			if (mTwoPane) {
				// In two-pane mode, show the detail view in this activity by
				// adding or replacing the detail fragment using a
				// fragment transaction.
				Bundle arguments = new Bundle();
				//arguments.putString(ReaderFragment.ARG_ITEM_ID, id);
				ReaderFragment fragment = new ReaderFragment();
				fragment.setArguments(arguments);
				getFragmentManager().beginTransaction()
						.replace(R.id.reader_container, fragment)
						.commit();
			} else {
				// In single-pane mode, simply start the reader activity
				// for the selected item ID.
				Intent detailIntent = new Intent(this, ReaderActivity.class);
				//detailIntent.putExtra(ReaderFragment.ARG_ITEM_ID, id);
				startActivity(detailIntent);
			}
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "CategoryPreviewFragment on CategoryPreviewFragment.Event", ex);
		}
	}
}
