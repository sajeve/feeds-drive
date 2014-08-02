package dh.newspaper.view;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import dh.newspaper.R;
import dh.newspaper.base.Injector;

public class ManageSubscriptionActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subscription);
		//((Injector)getApplication()).inject(this);

		getActionBar().setTitle(R.string.manage_subscription);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		/*
		workaround: cancel all scheduled Croutons.
		https://github.com/keyboardsurfer/Crouton
		 */
		Crouton.cancelAllCroutons();
	}

}
