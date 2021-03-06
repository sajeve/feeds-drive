package dh.newspaper.cache;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.BatteryManager;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import dh.newspaper.Constants;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoExecutorFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Referential data, Always cache in memory
 * Created by hiep on 3/06/2014.
 */
public class RefData {
	private static final String TAG = RefData.class.getName();
	//private List<PathToContent> mPathToContents;
	private volatile TreeSet<String> mActiveTags;
	private volatile TreeSet<String> mTags;
	private volatile LruDiscCache mLruDiscCache;
	//private boolean pathToContentsStale = false;
	//private boolean mTagsStale = false;
	private final Context mContext;
	private volatile List<Subscription> activeSubscriptions;
	private volatile List<Subscription> subscriptions;
	private final SharedPreferences mSharedPreferences;
	private final DatabaseHelper databaseHelper;

	public RefData(Context context, DatabaseHelper databaseHelper, SharedPreferences sharedPreferences) {
		mContext = context;
		mSharedPreferences = sharedPreferences;
		this.databaseHelper = databaseHelper;
	}

//	/**
//	 * Return list of enabled PathToContent order by priority
//	 */
//	public synchronized List<PathToContent> pathToContentList() {
//		if (mPathToContents==null || pathToContentsStale) {
//			checkAccessDiskOnMainThread();
//			Stopwatch sw = Stopwatch.createStarted();
//			mPathToContents = mDaoSession.getPathToContentDao().queryBuilder()
//					.where(PathToContentDao.Properties.Enable.eq(Boolean.TRUE))
//					.orderDesc(PathToContentDao.Properties.Priority)
//					.list();
//			Log.i(TAG, "Get PathToContent returned "+mPathToContents.size()+" records ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms)");
//		}
//		return mPathToContents;
//	}

	/**
	 * Must be called each time add/remove/update a subscription
	 * Get all possible tags from active subscription (alphabetic order)
	 */
	public synchronized void loadSubscriptionAndTags() {
		checkAccessDiskOnMainThread();
		Stopwatch sw = Stopwatch.createStarted();
		loadSubscriptions();
		loadTags();
		Log.i(TAG, "Found " + mActiveTags.size() + " active tags from " + getActiveSubscriptions().size() + " active subscriptions ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms)");
	}

	private void loadTags() {
		mActiveTags = new TreeSet<String>();
		mTags = new TreeSet<String>();

		for (Subscription sub : getSubscriptions()) {
			if (!TextUtils.isEmpty(sub.getTags())) {
				Iterable<String> subTags = Splitter.on('|').omitEmptyStrings().split(sub.getTags());
				for (String tag : subTags) {
					mTags.add(tag);
					if (sub.getEnable()) {
						mActiveTags.add(tag);
					}
				}
			}
		}
	}

	/**
	 * Must be called each time add/remove/update a subscription except you already call the {@link #loadSubscriptionAndTags()} which
	 * will call this one
	 * @return
	 */
	private void loadSubscriptions() {
		checkAccessDiskOnMainThread();

		/*SQLiteDatabase db = databaseHelper.getReadableDatabase();
		try {
			DaoSession daoSession = (new DaoMaster(db)).newSession();
			subscriptions = daoSession.getSubscriptionDao().loadAll();
		}
		finally {
			db.close();
		}*/

		databaseHelper.operate(new DatabaseHelper.DatabaseOperation() {
			@Override
			public void doOperate(DaoSession daoSession) {
				subscriptions = daoSession.getSubscriptionDao().loadAll();
			}
		});

		activeSubscriptions = new ArrayList<Subscription>();
		for (Subscription sub : subscriptions) {
			if (sub.getEnable()) {
				activeSubscriptions.add(sub);
			}
		}
		/*
		activeSubscriptions = mDaoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE))
				.list();
		*/
	}

	public List<Subscription> getActiveSubscriptions() {
		return activeSubscriptions;
	}
	public List<Subscription> getSubscriptions() {
		return subscriptions;
	}
	public Subscription findSubscription(String url) {
		url = StrUtils.removeTrailingSlash(url);
		for(Subscription sub : getSubscriptions()) {
			if (StrUtils.equalsIgnoreCases(StrUtils.removeTrailingSlash(sub.getFeedsUrl()), url)) {
				return sub;
			}
		}
		return null;
	}
	/**
	 * find all item which are already subscribed in a search result, set the subscription properties of the item
	 * to the corresponding subscription object in the database
	 * @param searchResult
	 */
	public void matchExistSubscriptions(SearchFeedsResult searchResult) {
		if (searchResult==null || searchResult.getResponseData()==null || searchResult.getResponseData().getEntries()==null) {
			return;
		}
		for (SearchFeedsResult.ResponseData.Entry entry : searchResult.getResponseData().getEntries()) {
			entry.setSubscription(findSubscription(entry.getUrl()));
		}
	}

	/*public boolean isTagsListReadyInMemory() {
		return mActiveTags!=null && !mTagsStale;
	}*/

	public TreeSet<String> getActiveTags() {
		/*if (activeSubscriptions == null) {
			loadSubscriptionAndTags();
		}*/
		return mActiveTags;
	}

	public TreeSet<String> getTags() {
		/*if (activeSubscriptions == null) {
			loadSubscriptionAndTags();
		}*/
		return mActiveTags;
	}

	public String getCachePath() {
		return getCachePath(mContext);
	}
	public static String getCachePath(Context context) {
		if (Constants.USE_DEBUG_DATABASE) {
			return Constants.DEBUG_DATABASE_PATH;
		}
		else {
			return context.getExternalCacheDir().getAbsolutePath(); /* /storage/emulated/0/Android/data/dh.newspaper/cache */
		}
	}

	/*public File getCacheDir() {
		if (Constants.DEBUG) {
			return new File(Constants.DEBUG_DATABASE_PATH);
		}
		return mContext.getExternalCacheDir();
	}*/

	private void checkAccessDiskOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Log.w(TAG, "Access disk on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

	private synchronized void setupLruDiscCache(long maxSize) {
		mLruDiscCache = new LruDiscCache(
				new File(getCachePath()+"/"+Constants.CACHE_IMAGE_FOLDER),
				DefaultConfigurationFactory.createFileNameGenerator(), maxSize);
	}

	public LruDiscCache getLruDiscCache() {
		if (mLruDiscCache == null) {
			checkAccessDiskOnMainThread();
			setupLruDiscCache(getPreferenceImageCacheSize());
		}
		return mLruDiscCache;
	}

	private DisplayImageOptions displayImageOptions;

	/**
	 * Init image loader using LruDiscCache
	 */
	public void initImageLoader() {
		if (displayImageOptions != null) {
			return; // init is already called
		}
		displayImageOptions = new DisplayImageOptions.Builder()
				.cacheInMemory(true)
				.cacheOnDisk(true)
				.build();
		ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(mContext)
				.diskCache(getLruDiscCache())
				//.diskCacheSize(getPreferenceImageCacheSize())
				.defaultDisplayImageOptions(displayImageOptions);

		if (Constants.DEBUG) {
			config.writeDebugLogs();
		}
		ImageLoader.getInstance().init(config.build());
	}

	//region preferences convenient getters

	public boolean getPreferenceServiceEnabled() {
		return getPreferenceServiceEnabled(mSharedPreferences);
	}
	public static boolean getPreferenceServiceEnabled(SharedPreferences sp) {
		return sp.getBoolean(Constants.PREF_SERVICE_ENABLED_KEY, Constants.PREF_SERVICE_ENABLED_DEFAULT);
	}

	public long getPreferenceServiceInterval() {
		return getPreferenceServiceInterval(mSharedPreferences);
	}
	public static long getPreferenceServiceInterval(SharedPreferences sp) {
		try {
			return Long.parseLong(sp.getString(Constants.PREF_INTERVALS_KEY, Constants.PREF_INTERVALS_DEFAULT));
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
			return 7200000L;
		}
	}

	public int getPreferenceNumberOfThread() {
		return getPreferenceNumberOfThread(mSharedPreferences);
	}
	public static int getPreferenceNumberOfThread(SharedPreferences sp) {
		try {
			return Integer.parseInt(sp.getString(Constants.PREF_DOWNLOADING_THREAD_KEY, Constants.PREF_DOWNLOADING_THREAD_DEFAULT));
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
			return 2;
		}
	}

	public long getPreferenceImageCacheSize() {
		return getPreferenceImageCacheSize(mSharedPreferences);
	}
	public static long getPreferenceImageCacheSize(SharedPreferences sp) {
		try {
			return Long.parseLong(sp.getString(Constants.PREF_IMAGE_CACHE_SIZE_KEY, Constants.PREF_IMAGE_CACHE_SIZE_DEFAULT));
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
			return 104857600L;
		}
	}

	public boolean getPreferenceOnlyRunServiceIfCharging() {
		return getPreferenceOnlyRunServiceIfCharging(mSharedPreferences);
	}
	public static boolean getPreferenceOnlyRunServiceIfCharging(SharedPreferences sp) {
		return sp.getBoolean(Constants.PREF_CHARGE_CONDITION_KEY, Constants.PREF_CHARGE_CONDITION_DEFAULT);
	}

	public boolean getPreferenceOnlineMode() {
		return getPreferenceOnlineMode(mSharedPreferences);
	}
	public static boolean getPreferenceOnlineMode(SharedPreferences sp) {
		return !sp.getBoolean(Constants.PREF_OFFLINE_KEY, Constants.PREF_OFFLINE_DEFAULT);
	}

	//endregion

	/**
	 * create executor with pool size base on preferences
	 */
	public PrifoExecutor createArticlesLoader(String name) {
		int threadsPoolSize = getPreferenceNumberOfThread();
		return PrifoExecutorFactory.newPrifoExecutor(name, threadsPoolSize);
	}
	public static PrifoExecutor createMainExecutor() {
		return PrifoExecutorFactory.newPrifoExecutor("Main", 1);
	}

	/**
	 * setup pool size base on preferences
	 * @param articlesLoader
	 */
	public void updateArticleLoaderPoolSize(PrifoExecutor articlesLoader) {
		if (articlesLoader==null) {
			return;
		}
		int threadsPoolSize = getPreferenceNumberOfThread();
		articlesLoader.setCorePoolSize(threadsPoolSize);
	}

	public boolean isBatteryCharging() {
		Intent batteryStatus = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
	}

//	public DaoMaster createReadOnlyDaoMaster() {
//		return new DaoMaster(databaseHelper.createReadOnlyDatabase());
//	}
//	public DaoMaster createWritableDaoMaster() {
//		return new DaoMaster(databaseHelper.createWritableDatabase());
//	}

}
