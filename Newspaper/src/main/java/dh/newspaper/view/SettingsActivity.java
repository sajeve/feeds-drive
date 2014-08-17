package dh.newspaper.view;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;
import android.widget.Toast;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.services.AlarmReceiver;
import dh.tool.common.StrUtils;

import javax.inject.Inject;

public class SettingsActivity extends Activity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
    }
}
