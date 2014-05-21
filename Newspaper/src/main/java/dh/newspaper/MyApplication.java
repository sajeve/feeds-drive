package dh.newspaper;

import android.app.FragmentManager;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import com.google.common.io.Files;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.ArticleDao;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.modules.GlobalModule;
import dh.newspaper.view.utils.ErrorDialogFragment;

import javax.inject.Inject;
import java.io.File;
import java.util.*;

public class MyApplication extends InjectingApplication {
	private static final String TAG = MyApplication.class.getName();

	@Inject
	AppBundle mAppBundle;

	SQLiteDatabase db;
	DaoMaster daoMaster;
	DaoSession daoSession;

	@Override
	public void onCreate() {
		super.onCreate();

		if (Constants.DEBUG) {
			StrictMode.enableDefaults();
		}

		DaoMaster.OpenHelper helper = new DaoMaster.DevOpenHelper((Context)this, Constants.DATABASE_NAME, null); //TODO debug only (because drops all tables)
		db = helper.getWritableDatabase();
		daoMaster = new DaoMaster(db);
		daoSession = daoMaster.newSession();

		Log.i(TAG,"Database Path = " + db.getPath());

		try {
			Article article = new Article(null, "articleUrl", "parentUrl", "imageUrl", "title", "author", "excerpt", "content", "vn", new Date(), null, null, new Date());
			daoSession.getArticleDao().insert(article);
		} catch (Exception ex) {
			Log.w(TAG, ex.getMessage());
		}

		EventBus.getDefault().register(mAppBundle, 100);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		EventBus.getDefault().unregister(mAppBundle);
		db.close();
	}

	@Override
	public File getDatabasePath(String name) {
		return new File(getDatabasePathString(name));
	}

	public String getDatabasePathString(String name) {
		if (Constants.DEBUG) {
			return "/mnt/shared/bridge/"+name+".db";
		}
		else {
			return getExternalCacheDir()+ "/" + name+".db";
		}
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
	public String[] databaseList() {
		return new String[]{Constants.DATABASE_NAME};
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