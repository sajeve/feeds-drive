package dh.newspaper.modules;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import dagger.Module;
import dagger.Provides;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.adapter.ArticlesGridAdapter;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.services.BackgroundTasksManager;
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

	private Context mAppContext;
	//private RequestQueue mRequestQueue;
	private SQLiteOpenHelper mDbHelper;
	private SQLiteDatabase mDatabase;
	private DaoMaster mDaoMaster;
	private DaoSession mDaoSession;
	private RefData mRefData;
	private BackgroundTasksManager mBackgroundTasksManager;
	private boolean isImageLoaderInitialized = false;

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
	public SQLiteDatabase provideDatabase() {
		if (mDatabase == null) {
			//SQLiteOpenHelper mDbHelper = new DaoMaster.DevOpenHelper((Context)this, Constants.DATABASE_NAME, null); //debug only (because drops all tables)
			mDbHelper = new DatabaseHelper(mAppContext); //upgrade with the database in assets
			mDatabase = mDbHelper.getReadableDatabase();
		}
		return mDatabase;
	}

	@Provides @Singleton
	public DaoMaster provideDaoMaster() {
		if (mDaoMaster == null) {
			mDaoMaster = new DaoMaster(provideDatabase());
		}
		return mDaoMaster;
	}

	@Provides @Singleton
	public DaoSession provideDaoSession() {
		if (mDaoSession == null) {
			mDaoSession = provideDaoMaster().newSession();
		}
		return mDaoSession;
	}

	@Provides @Singleton
	public RefData provideRefData() {
		if (mRefData == null) {
			mRefData = new RefData(provideDaoSession(), mAppContext);
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
}