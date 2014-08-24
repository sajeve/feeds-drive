package dh.newspaper.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.services.AlarmReceiver;
import dh.tool.common.StrUtils;

import javax.inject.Inject;

/**
 * Preferences
 */
public class SettingsFragment extends PreferenceFragment {
	private static final String TAG = SettingsFragment.class.getName();

	@Inject SharedPreferences sharedPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_main);
	}

	SharedPreferences.OnSharedPreferenceChangeListener spl = new SharedPreferences.OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			try {
				if (!isAdded()) {
					return;
				}

				Context appContext = getActivity().getApplicationContext();
				if (StrUtils.equalsString(key, Constants.PREF_SERVICE_ENABLED_KEY)) {
					boolean isEnable = RefData.getPreferenceServiceEnabled(sharedPreferences);
					AlarmReceiver.setAlarmEnable(appContext, isEnable);
					return;
				}

				if (StrUtils.equalsString(key, Constants.PREF_CHARGE_CONDITION_KEY)) {
					return;
				}

				if (StrUtils.equalsString(key, Constants.PREF_INTERVALS_KEY)) {
					long interval = RefData.getPreferenceServiceInterval(sharedPreferences);
					AlarmReceiver.setupAlarm(appContext, interval, interval);
				}

				//change resume
				Preference p = findPreference(key);
				p.setSummary(sharedPreferences.getString(key, ""));
			}
			catch (Exception ex) {
				Log.w(TAG, ex);
				MyApplication.showErrorDialog(getFragmentManager(), "onSharedPreferenceChanged", ex);
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		sharedPreferences.registerOnSharedPreferenceChangeListener(spl);
	}

	@Override
	public void onPause() {
		super.onPause();
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(spl);
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
		}
	}
}
