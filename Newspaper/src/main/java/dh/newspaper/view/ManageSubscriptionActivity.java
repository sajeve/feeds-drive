package dh.newspaper.view;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.adapter.SubscriptionListAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SaveSubscriptionEvent;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.view.utils.BgCheckbox;
import dh.tool.common.StrUtils;

import javax.inject.Inject;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ManageSubscriptionActivity extends ListActivity {
	private static final String TAG = ManageSubscriptionActivity.class.getName();

	@Inject RefData refData;
	//@Inject DaoSession daoSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_subscription);
		((Injector)getApplication()).inject(this);
		getActionBar().setTitle(R.string.manage_subscription);
		setListAdapter(new SubscriptionListAdapter(this, Executors.newCachedThreadPool(), editCategory, toggleSubscription));
		//((SubscriptionListAdapter)getListAdapter()).setData(refData.getSubscriptions());
    }

	/*@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
	}*/

	private View.OnClickListener editCategory = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Subscription sub = (Subscription)v.getTag();
				SubscriptionDialog subDlg = SubscriptionDialog.newInstance(sub);
				subDlg.show(getFragmentManager(), SubscriptionDialog.class.getName());
			} catch (Exception ex) {
				Log.w(TAG, ex);
				MyApplication.showErrorDialog(getFragmentManager(), "onClick", ex);
			}
		}
	};

	private BgCheckbox.ICheckedAction toggleSubscription = new BgCheckbox.ICheckedAction() {
		@Override
		public boolean performActionInBackground(boolean isChecked, Object senderTag) {
			DaoMaster daoMaster = refData.createWritableDaoMaster();
			try {
				DaoSession daoSession = daoMaster.newSession();
				Subscription sub = (Subscription)senderTag;
				sub.setEnable(isChecked);
				daoSession.getSubscriptionDao().update(sub);
				return true;
			} catch (Exception ex) {
				Log.w(TAG, ex);
				return false;
			}
			finally {
				try {
					daoMaster.getDatabase().close();
				}
				catch (Exception ex) {
					Log.wtf(TAG, "Cannot close database", ex);
				}
			}
		}

		@Override
		public void onFinished(Object sender, boolean success) {
			try {
				if (success) {
					((SubscriptionListAdapter)ManageSubscriptionActivity.this.getListAdapter()).notifyDataSetChanged();
				}
				else {
					Crouton.makeText(ManageSubscriptionActivity.this, "Failed to activate/deactivate the subscription", Style.ALERT).show(); //TODO translate
				}

			} catch (Exception ex) {
				Log.w(TAG, ex);
				MyApplication.showErrorDialog(getFragmentManager(), "onCheckedChanged", ex);
			}
		}
	};

	public void onEventMainThread(SaveSubscriptionEvent event) {
		try {
			if (StrUtils.equalsString(Constants.SUBJECT_SAVE_SUBSCRIPTION_DONE, event.getSubject())) {
				((SubscriptionListAdapter)getListAdapter()).setData(refData.getSubscriptions());
			}
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.manage_subscription, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
			case R.id.action_subscribe:
				startActivity(new Intent(this, SubscriptionActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
		((SubscriptionListAdapter)getListAdapter()).setData(refData.getSubscriptions());
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
