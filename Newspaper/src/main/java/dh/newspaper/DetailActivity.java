package dh.newspaper;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import dh.newspaper.base.Injector;
import dh.newspaper.model.generated.Article;
import dh.newspaper.view.ArticleFragment;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.view.TagsFragment;

import javax.inject.Inject;

public class DetailActivity extends Activity {
	private static final String TAG = DetailActivity.class.getName();

	private boolean mSinglePane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
		//((Injector) getApplication()).inject(this);

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

		mSinglePane = findViewById(R.id.fragment_feeds) == null;
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
}
