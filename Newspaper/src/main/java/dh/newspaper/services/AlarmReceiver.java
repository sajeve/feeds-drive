package dh.newspaper.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hiep on 6/07/2014.
 */
public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = AlarmReceiver.class.getName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Recurring alarm; requesting download service.");
		Intent downloader = new Intent(context, FeedsDownloaderService.class);
		context.startService(downloader);
	}

}
