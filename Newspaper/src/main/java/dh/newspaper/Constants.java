package dh.newspaper;

import android.app.AlarmManager;
import dh.newspaper.model.generated.Article;
import net.danlew.android.joda.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Calendar;

/**
 * Created by hiep on 8/05/2014.
 */
public class Constants {
	public static final boolean DEBUG = false;
	public static final boolean LOAD_FIRST_TAG_ON_START = false;
	public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
	public static final String DEBUG_DATABASE_PATH = "/mnt/shared/bridge";
	public static final String DATABASE_NAME = "newspaper";
	public static final int DATABASE_VERSION = 102;
	public static final String CACHE_IMAGE_FOLDER = "images";
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final int EXCERPT_LENGTH = 320;
	public static final Duration ARTICLE_TTL = DEBUG ? new Duration(100) : new Duration(1000); //1s
	public static final Duration SUBSCRIPTION_TTL = DEBUG ? new Duration(100) : new Duration(1000); //1s
	//public static final Duration INFINITE_DURATION = new Duration(Long.MAX_VALUE);

	/**
	 * number of actual articles in memory cache, it is the windows size of the list view
	 */
	public static final int ARTICLES_PER_PAGE = 100;

	//public static final int IMAGE_DISK_CACHE_SIZE = 100 * 1024 * 1024;
	//public static final int THREAD_ARTICLES_LOADER = 2;

	public static final String SUBJECT_TAGS_START_LOADING = "Tags.StartLoading";
	public static final String SUBJECT_TAGS_END_LOADING = "Tags.Refresh";
	public static final String SUBJECT_SEARCH_FEEDS_START_LOADING = "SearchFeeds.StartLoading";
	public static final String SUBJECT_SEARCH_FEEDS_REFRESH = "SearchFeeds.Refresh";
	public static final String SUBJECT_SEARCH_FEEDS_DONE_LOADING = "SearchFeeds.DoneLoading";
	public static final String SUBJECT_FEEDS_START_LOADING = "Feeds.StartLoading";
	public static final String SUBJECT_FEEDS_REFRESH = "Feeds.Refresh";
	public static final String SUBJECT_FEEDS_DONE_LOADING = "Feeds.DoneLoading";
	public static final String SUBJECT_ARTICLE_START_LOADING = "Article.StartLoading";
	public static final String SUBJECT_ARTICLE_REFRESH = "Article.Refresh";
	public static final String SUBJECT_ARTICLE_DISPLAY_FULL_WEBPAGE = "Article.DisplayFullWebPage";
	public static final String SUBJECT_ARTICLE_DONE_LOADING = "Article.DoneLoading";
	public static final String SUBJECT_SAVE_SUBSCRIPTION_PROGRESS_MESSAGE = "SaveSubscription.SendProgressMessage";
	public static final String SUBJECT_SAVE_SUBSCRIPTION_DONE = "SaveSubscription.Done";
	public static final String SUBJECT_SAVE_SUBSCRIPTION_ERROR = "SaveSubscription.SendError";

	public static final int EVENT_DELAYED = 200;
	public static final String ACTIONBAR_TITLE = "ActionBar.Title";

	/**
	 * we only update article content in cache if the new content length is at least
	 * around 90% of the existing content in cache
	 * Negative number = do not use this fixture
	 */
	public static final int ARTICLE_LENGTH_PERCENT_TOLERANT = 50; //90

	public static final int ARTICLE_MIN_LENGTH = 200; //90

	//public static final boolean ENABLE_ALARM = true;
	//public static final long SERVICE_INTERVAL = DEBUG ? 5*60*1000 : 2*AlarmManager.INTERVAL_HOUR; //AlarmManager.INTERVAL_DAY
	public static final long SERVICE_START_AT = 2000; //updateTime.getTimeInMillis(),

	public static final String PREF_OFFLINE_KEY = "Offline";
	public static final boolean PREF_OFFLINE_DEFAULT = false;
	public static final String PREF_SERVICE_ENABLED_KEY = "pref_serviceEnabled";
	public static final boolean PREF_SERVICE_ENABLED_DEFAULT = true;
	public static final String PREF_INTERVALS_KEY = "pref_intervals";
	public static final String PREF_INTERVALS_DEFAULT = "7200000";
	public static final String PREF_NETWORK_CONDITION_KEY = "pref_networkCondition";
	public static final String PREF_NETWORK_CONDITION_DEFAULT = "wifi";
	public static final String PREF_CHARGE_CONDITION_KEY = "pref_chargeCondition";
	public static final boolean PREF_CHARGE_CONDITION_DEFAULT = false;
	public static final String PREF_DOWNLOADING_THREAD_KEY = "pref_downloadingThread";
	public static final String PREF_DOWNLOADING_THREAD_DEFAULT = "2";
	public static final String PREF_IMAGE_CACHE_SIZE_KEY = "pref_imageDiskCacheSize";
	public static final String PREF_IMAGE_CACHE_SIZE_DEFAULT = "104857600"; //100 MB
}
