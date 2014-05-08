package dh.newspaper;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import dagger.ObjectGraph;
import dh.newspaper.modules.ParserModule;
import dh.newspaper.view.ErrorDialogFragment;

import java.util.Calendar;

public class MyApplication extends Application {

	ObjectGraph objectGraph;

	@Override
	public void onCreate() {
		super.onCreate();
		if (Constants.DEBUG) {
			StrictMode.enableDefaults();
		}

		this.objectGraph = ObjectGraph.create(new ParserModule());
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

	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}

	public ObjectGraph getObjectGraph() {
		return objectGraph;
	}
}