package dh.newspaper.cache;

import android.content.Context;
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
import dh.newspaper.model.generated.*;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.tool.common.StrUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Referential data, Always cache in memory
 * Created by hiep on 3/06/2014.
 */
public class RefData {
	private static final String TAG = RefData.class.getName();
	private final DaoSession mDaoSession;
	//private List<PathToContent> mPathToContents;
	private TreeSet<String> mTags;
	private LruDiscCache mLruDiscCache;
	//private boolean pathToContentsStale = false;
	//private boolean mTagsStale = false;
	private Context mContext;
	private List<Subscription> activeSubscriptions;
	private List<Subscription> subscriptions;

	@Inject
	public RefData(DaoSession daoSession, Context context) {
		mDaoSession = daoSession;
		mContext = context;
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
	public synchronized TreeSet<String> loadTags() {
		checkAccessDiskOnMainThread();
		Stopwatch sw = Stopwatch.createStarted();
		loadSubscriptions();
		mTags = new TreeSet<String>();

		for (Subscription sub : getActiveSubscriptions()) {
			if (!TextUtils.isEmpty(sub.getTags())) {
				Iterable<String> subTags = Splitter.on('|').omitEmptyStrings().split(sub.getTags());
				for (String tag : subTags) {
					mTags.add(tag);
				}
			}
		}
		Log.i(TAG, "Found " + mTags.size() + " tags from " + getActiveSubscriptions().size() + " active subscriptions ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms)");
		return mTags;
	}

	/**
	 * Must be called each time add/remove/update a subscription except you already call the {@link #loadTags()} which
	 * will call this one
	 * @return
	 */
	public synchronized void loadSubscriptions() {
		checkAccessDiskOnMainThread();

		subscriptions = mDaoSession.getSubscriptionDao().loadAll();
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
		return mTags!=null && !mTagsStale;
	}*/

	public TreeSet<String> getTags() {
		/*if (activeSubscriptions == null) {
			loadTags();
		}*/
		return mTags;
	}

	public String getCachePath() {
		if (Constants.DEBUG) {
			return Constants.DEBUG_DATABASE_PATH;
		}
		return mContext.getExternalCacheDir().getAbsolutePath();
	}

	public File getCacheDir() {
		if (Constants.DEBUG) {
			return new File(Constants.DEBUG_DATABASE_PATH);
		}
		return mContext.getExternalCacheDir();
	}

	private void checkAccessDiskOnMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Log.w(TAG, "Access disk on main thread");
			if (Constants.DEBUG) {
				throw new IllegalStateException("Access disk on main thread");
			}
		}
	}

	private synchronized void setupLruDiscCache(int maxSize) {
		mLruDiscCache = new LruDiscCache(
				new File(getCachePath()+"/"+Constants.CACHE_IMAGE_FOLDER),
				DefaultConfigurationFactory.createFileNameGenerator(), maxSize);
	}

	public LruDiscCache getLruDiscCache() {
		if (mLruDiscCache == null) {
			setupLruDiscCache(Constants.IMAGE_DISK_CACHE_SIZE);
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
				.diskCacheSize(Constants.IMAGE_DISK_CACHE_SIZE)
				.defaultDisplayImageOptions(displayImageOptions);

		if (Constants.DEBUG) {
			config.writeDebugLogs();
		}
		ImageLoader.getInstance().init(config.build());
	}
}
