package dh.newspaper.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.cache.RefData;

/**
 * Created by hiep on 6/07/2014.
 */
public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = AlarmReceiver.class.getName();

//	public static void cancelAlarm(Context context) {
//		PendingIntent recurringDownload = getDownloadIntent(context);
//		AlarmManager alarms = getAlarmManager(context);
//
//		//cancel pending task
//		try {
//			alarms.cancel(recurringDownload);
//		}
//		catch (Exception ex) {
//			Log.w(TAG, ex);
//		}
//	}

	public static void setupAlarm(Context context, long startAt, long interval) {
		PendingIntent recurringDownload = getDownloadIntent(context);
		AlarmManager alarms = getAlarmManager(context);
		try {
			//cancel pending task
			alarms.cancel(recurringDownload);

			//stop service
			Intent downloadService = new Intent(context, FeedsDownloaderService.class);
			context.stopService(downloadService);
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}

		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				startAt,
				interval,
				recurringDownload);

		Log.i("ALARM", String.format("setupAlarm(%d, %d)", startAt, interval));
	}

	private static PendingIntent getDownloadIntent(Context context) {
		Intent downloader = new Intent(context, AlarmReceiver.class);
		PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
				0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
		return recurringDownload;
	}
	private static AlarmManager getAlarmManager(Context context) {
		return (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
	}

	public static void setAlarmEnable(Context context, boolean enable) {
		ComponentName receiver = new ComponentName(context.getApplicationContext(), AlarmReceiver.class);
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(receiver,
				enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);

		Log.i("ALARM", String.format("setAlarmEnable(%s)", enable));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent!=null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			RefData refData = ((InjectingApplication)context).getObjectGraph().get(RefData.class);
			if (refData.getPreferenceServiceEnabled()) {
				setupAlarm(context, Constants.SERVICE_START_AT, refData.getPreferenceServiceInterval());
			}
		}
		Log.d(TAG, "Recurring alarm; requesting download service.");
		Intent downloadService = new Intent(context, FeedsDownloaderService.class);
		context.startService(downloadService);
	}

}
