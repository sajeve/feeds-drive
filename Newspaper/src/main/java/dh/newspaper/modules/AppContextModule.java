package dh.newspaper.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.view.FeedsFragment;
import dh.newspaper.view.TagsFragment;

import javax.inject.Singleton;

/**
 * Created by hiep on 8/05/2014.
 */
@Module (
	injects = {
			MyApplication.class,
			FeedsFragment.class,
			TagsFragment.class,
			BackgroundTasksManager.class,
		},
		library = true
)
public class AppContextModule {

	private final Context mAppContext;
	//private RequestQueue mRequestQueue;
	private volatile DatabaseHelper mDbHelper;
	/*private SQLiteDatabase mDatabase;
	private DaoMaster mDaoMaster;
	private DaoSession mDaoSession;*/
	private volatile RefData mRefData;
	private volatile BackgroundTasksManager mBackgroundTasksManager;

	public AppContextModule(Context mAppContext) {
		this.mAppContext = mAppContext;
	}

	/*@Provides @Singleton
	public RequestQueue provideRequestQueue() {
		if (mRequestQueue == null) {
			mRequestQueue = Volley.newRequestQueue(mAppContext, new OkHttpStack());
		}
		return mRequestQueue;
	}*/

	@Provides @Singleton
	public DatabaseHelper provideDatabaseHelper() {
		if (mDbHelper == null) {
			mDbHelper = new DatabaseHelper(mAppContext); //install with the database in assets
		}
		return mDbHelper;
	}

//	@Provides @Singleton
//	public SQLiteDatabase provideDatabase() {
//		if (mDatabase == null) {
//			mDatabase = provideDatabaseHelper().getReadableDatabase();
//		}
//		return mDatabase;
//	}

//	@Provides @Singleton
//	public DaoMaster provideDaoMaster() {
//		if (mDaoMaster == null) {
//			mDaoMaster = new DaoMaster(provideDatabase());
//		}
//		return mDaoMaster;
//	}

//	@Provides @Singleton
//	public DaoSession provideDaoSession() {
//		if (mDaoSession == null) {
//			mDaoSession = provideDaoMaster().newSession();
//		}
//		return mDaoSession;
//	}

	@Provides @Singleton
	public RefData provideRefData() {
		if (mRefData == null) {
			mRefData = new RefData(mAppContext, provideDatabaseHelper(), providePreferences());
		}
		return mRefData;
	}

	@Provides @Singleton
	public BackgroundTasksManager provideBackgroundTasksManager() {
		if (mBackgroundTasksManager == null) {
			mBackgroundTasksManager = new BackgroundTasksManager(mAppContext);
		}
		return mBackgroundTasksManager;
	}

	SharedPreferences mPreferences;

	@Provides @Singleton
	public SharedPreferences providePreferences() {
		if (mPreferences == null) {
			mPreferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
		}
		return mPreferences;
	}

	@Provides @Singleton
	public ObjectMapper provideObjectMapper() {
		return new ObjectMapper();
	}

	@Provides @Singleton
	public ConnectivityManager provideConnectivityManager() {
		return (ConnectivityManager)mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	@Provides
	public Context provideContext() {
		return mAppContext;
	}
}