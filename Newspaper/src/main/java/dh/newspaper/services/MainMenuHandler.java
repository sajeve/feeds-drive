package dh.newspaper.services;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.MenuItem;
import dh.newspaper.Constants;
import dh.newspaper.R;
import dh.newspaper.view.SettingsActivity;

import javax.inject.Inject;

/**
 * Created by hiep on 18/07/2014.
 */
public class MainMenuHandler {
	private static final String TAG = MainMenuHandler.class.getName();
	SharedPreferences mSharedPreferences;

	@Inject
	public MainMenuHandler(SharedPreferences sharedPreferences) {
		mSharedPreferences = sharedPreferences;
	}

	public boolean onOptionsItemSelected(Activity owner, MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case R.id.action_offline:
				boolean newState = !item.isChecked();
				item.setChecked(newState);
				Log.d(TAG, "Switch to " + (newState ? "offline" : "online") + " mode");
				mSharedPreferences.edit().putBoolean(Constants.PREF_OFFLINE_KEY, newState).apply();
				return true;
			case R.id.action_settings:
				owner.startActivity(new Intent(owner, SettingsActivity.class));
				return true;
		}
		return false;
	}
}
