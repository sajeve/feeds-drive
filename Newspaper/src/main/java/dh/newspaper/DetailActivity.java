package dh.newspaper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import dh.newspaper.base.Injector;
import dh.newspaper.event.RawEvent;
import dh.newspaper.model.generated.Article;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.services.MainMenuHandler;
import dh.newspaper.view.ArticleFragment;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.view.TagsFragment;

import javax.inject.Inject;

public class DetailActivity extends Activity {
	private static final String TAG = DetailActivity.class.getName();

	private boolean mSinglePane;

	@Inject SharedPreferences mSharedPreferences;
	@Inject BackgroundTasksManager mBackgroundTasksManager;

	@Inject MainMenuHandler mMainMenuHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
		((Injector) getApplication()).inject(this);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(mTitle);

		mSinglePane = findViewById(R.id.fragment_feeds) == null;

		overridePendingTransition(R.anim.right_in, R.anim.left_out);

		/*if (mSinglePane) {
			getFragmentManager().beginTransaction()
					.replace(R.id.fragment_article, mArticleFragment)
					.commit();
		}
		else {
			getFragmentManager().beginTransaction()
					.replace(R.id.fragment_feeds, mFeedsFragment)
					.replace(R.id.fragment_article, mArticleFragment)
					.commit();
		}*/

        // TODO: If exposing deep links into your app, handle intents here.
    }

	@Override
	protected void onStart() {
		super.onStart();
		mTitle = getIntent().getStringExtra(Constants.ACTIONBAR_TITLE);
		getActionBar().setTitle(mTitle);
	}


	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.left_in, R.anim.right_out);
	}

	/**
	 * Used to store the last screen title. For use in
	 */
	private CharSequence mTitle;

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


	/**
	 * Menu
	 */
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



		getMenuInflater().inflate(R.menu.detail, menu);

		//restore menu state
		MenuItem offlineItem = menu.findItem(R.id.action_offline);
		offlineItem.setChecked(mSharedPreferences.getBoolean(Constants.PREF_OFFLINE, Constants.PREF_OFFLINE_DEFAULT));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (mMainMenuHandler.onOptionsItemSelected(this, item)) {
			return true;
		}

		if (id == android.R.id.home) {
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			overridePendingTransition(R.anim.left_in, R.anim.right_out);
			return true;
		}

		if (id == R.id.show_original) {
			if (mBackgroundTasksManager.getActiveSelectArticleWorkflow() == null) {
				Crouton.makeText(this, "No article selected", Style.ALERT).show(); //TODO translate
				return true;
			}

			Article currentArticle = mBackgroundTasksManager.getActiveSelectArticleWorkflow().getArticle();

			if (Constants.DEBUG) {
				if (currentArticle == null) {
					throw new IllegalStateException("Workflow null article");
				}
			}

			EventBus.getDefault().post(new RawEvent(Constants.SUBJECT_ARTICLE_DISPLAY_FULL_WEBPAGE));
		}

		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Crouton.cancelAllCroutons();
	}
}
