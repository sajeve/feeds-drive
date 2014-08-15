package dh.newspaper.view;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import dh.newspaper.R;

/**
 * Preferences
 */
public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences_main);
	}
}
