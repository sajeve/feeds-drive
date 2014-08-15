package dh.newspaper;

import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import de.psdev.slf4j.android.logger.AndroidLoggerAdapter;
import de.psdev.slf4j.android.logger.LogLevel;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.modules.AppContextModule;
import dh.newspaper.modules.GlobalModule;
import dh.newspaper.services.AlarmReceiver;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.view.utils.ErrorDialogFragment;
import net.danlew.android.joda.ResourceZoneInfoProvider;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyApplication extends InjectingApplication {
	private static final String TAG = MyApplication.class.getName();

	@Inject RefData mRefData;

	@Inject BackgroundTasksManager mBackgroundTasksManager;

	@Override
	public void onCreate() {
		super.onCreate();

		if (Constants.DEBUG) {
			StrictMode.enableDefaults();
		}

		AndroidLoggerAdapter.setLogLevel(Constants.DEBUG ? LogLevel.DEBUG : LogLevel.INFO);

		ResourceZoneInfoProvider.init(this);
		mBackgroundTasksManager.runInitialisationWorkflow();

		if (Constants.ENABLE_ALARM) {
			setupAlarm(getApplicationContext(), Constants.SERVICE_START_AT, Constants.SERVICE_INTERVAL);
		}
	}

	@Override
	public void onTerminate() {
		try {
			//EventBus.getDefault().unregister(mAppBundle);
			mBackgroundTasksManager.close();
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}
		super.onTerminate();
	}

	@Override
	public File getDatabasePath(String name) {
		return new File(getDatabasePathString(name));
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
		return super.openOrCreateDatabase(getDatabasePathString(name), mode, factory);
	}

	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
		return super.openOrCreateDatabase(getDatabasePathString(name), mode, factory, errorHandler);
	}

	@Override
	public File getCacheDir() {
		return mRefData.getCacheDir();
	}

	private void setupAlarm(Context context, long startAt, long interval) {
		// let's grab new stuff at around 11:45 GMT, inexactly
		/*Calendar updateTime = Calendar.getInstance();
		updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
		updateTime.set(Calendar.HOUR_OF_DAY, 11);
		updateTime.set(Calendar.MINUTE, 45);*/

		Intent downloader = new Intent(context, AlarmReceiver.class);
		PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
				0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

		try {
			alarms.cancel(recurringDownload);
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}

		alarms.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				startAt,
				interval,
				recurringDownload);
	}

	public String getDatabasePathString(String name) {
		return getCacheDir()+"/"+name+".mDb";
	}

	public static void showErrorDialog(final FragmentManager fm, final String message, final Throwable ex) {
		try {
			//Log.w(TAG, message, ex);
			if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
				showErrorDialogOnMainThread(fm, message, ex);
			}
			else {
				Handler mainThread = new Handler(Looper.getMainLooper());
				mainThread.post(new Runnable() {
					@Override
					public void run() {
						try {
							showErrorDialogOnMainThread(fm, message, ex);
						}
						catch (Exception ex1) {
							Log.wtf(TAG, ex1);
						}
					}
				});
			}
		}
		catch (Exception ex2) {
			Log.wtf(TAG, ex2);
		}
	}

	private static void showErrorDialogOnMainThread(final FragmentManager fm,
			final String message, final Throwable ex)
	{
		ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(message, ex);
		dialog.show(fm, "ReportErrorDialog "+ DateTime.now());
	}

	@Override
	protected List<Object> getModules() {
		return new ArrayList<Object>(){{
			add(new GlobalModule());
			add(new AppContextModule(getApplicationContext()));
		}};
	}
}