package dh.newspaper;

import android.app.FragmentManager;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.modules.GlobalModule;
import dh.newspaper.view.utils.ErrorDialogFragment;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class MyApplication extends InjectingApplication {

	@Inject
	AppBundle mAppBundle;

	@Override
	public void onCreate() {
		super.onCreate();
		if (Constants.DEBUG) {
			StrictMode.enableDefaults();
		}
		EventBus.getDefault().register(mAppBundle, 100);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		EventBus.getDefault().unregister(mAppBundle);
	}

	public static void showErrorDialog(final FragmentManager fm, final String message, final Throwable ex) {
		try {
			Log.e("dh.newspaper", message, ex);
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
							Log.wtf("MyApp", ex1);
						}
					}
				});
			}
		}
		catch (Exception ex2) {
			Log.wtf("MyApp", ex2);
		}
	}

	private static void showErrorDialogOnMainThread(final FragmentManager fm,
			final String message, final Throwable ex)
	{
		ErrorDialogFragment dialog = ErrorDialogFragment.newInstance(message, ex);
		dialog.show(fm, "ReportErrorDialog "+getNow());
	}

	@Override
	protected List<Object> getModules() {
		return new ArrayList<Object>(){{add(new GlobalModule());}};
	}

	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}
}