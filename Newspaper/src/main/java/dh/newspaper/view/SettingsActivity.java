package dh.newspaper.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import dh.newspaper.Constants;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.services.AlarmReceiver;
import dh.tool.common.StrUtils;

import javax.inject.Inject;

public class SettingsActivity extends Activity {
	@Inject SharedPreferences sharedPreferences;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		((Injector)getApplication()).inject(this);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
    }


	SharedPreferences.OnSharedPreferenceChangeListener spl = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Context appContext = SettingsActivity.this.getApplicationContext();
			/*if (StrUtils.equalsString(key, Constants.PREF_SERVICE_ENABLED_KEY) ||
					StrUtils.equalsString(key, Constants.PREF_INTERVALS_KEY)) {
				Toast.makeText(SettingsActivity.this, String.format("Preference changed %s", key), Toast.LENGTH_SHORT).show();
			}*/
			if (StrUtils.equalsString(key, Constants.PREF_SERVICE_ENABLED_KEY)) {
				boolean isEnable = RefData.getPreferenceServiceEnabled(sharedPreferences);
				AlarmReceiver.setAlarmEnable(appContext, isEnable);
			}
			if (StrUtils.equalsString(key, Constants.PREF_INTERVALS_KEY)) {
				long interval = RefData.getPreferenceServiceInterval(sharedPreferences);
				AlarmReceiver.setupAlarm(appContext, interval, interval);
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		sharedPreferences.registerOnSharedPreferenceChangeListener(spl);
	}

	@Override
	protected void onPause() {
		super.onPause();
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(spl);
	}
}
