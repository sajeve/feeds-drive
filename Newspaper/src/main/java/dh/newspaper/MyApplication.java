package dh.newspaper;

import android.app.FragmentManager;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import de.greenrobot.event.EventBus;
import dh.newspaper.base.InjectingApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.modules.AppContextModule;
import dh.newspaper.modules.GlobalModule;
import dh.newspaper.parser.ContentParser;
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

		ResourceZoneInfoProvider.init(this);
		mBackgroundTasksManager.runInitialisationWorkflow();

/*
		mDb = mDbHelper.getWritableDatabase();

		mDaoMaster = new DaoMaster(mDb);
		mDaoSession = mDaoMaster.newSession();
		Log.i(TAG, "Database Path = " + mDb.getPath());

		try {
			Article article = new Article(null, "articleUrl1", "parentUrl1", "imageUrl1", "title1", "author1", "excerpt1", "content1", "fa1256", "en", 0L, null, null, null, null, new Date());
			mDaoSession.getArticleDao().insert(article);
		} catch (Exception ex) {
			Log.w(TAG, ex.getMessage());
		}
*/
		//EventBus.getDefault().register(mAppBundle, 100);
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

	public String getDatabasePathString(String name) {
		return mRefData.getCachePath()+"/"+name+".mDb";
	}

	public static void showErrorDialog(final FragmentManager fm, final String message, final Throwable ex) {
		try {
			Log.e(TAG, message, ex);
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