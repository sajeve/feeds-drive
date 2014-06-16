package dh.newspaper;

import dh.newspaper.model.generated.Article;
import net.danlew.android.joda.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.Calendar;

/**
 * Created by hiep on 8/05/2014.
 */
public class Constants {
	public static final boolean DEBUG = true;
	public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
	public static final String DEBUG_DATABASE_PATH = "/mnt/shared/bridge";
	public static final String DATABASE_NAME = "newspaper";
	public static final int DATABASE_VERSION = 100;
	public static final String CACHE_IMAGE_FOLDER = "images";
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final int EXCERPT_LENGTH = 320;
	public static final Duration ARTICLE_TTL = DEBUG ? new Duration(100) : new Duration(1000); //1s
	public static final Duration SUBSCRIPTION_TTL = DEBUG ? new Duration(100) : new Duration(1000); //1s
	public static final int ARTICLES_PER_PAGE = DEBUG ? 100 : 200;

	public static final int IMAGE_DISK_CACHE_SIZE = 100 * 1024 * 1024;
	public static final int THREAD_ARTICLES_LOADER = 1;

	public static final String SUBJECT_TAGS_START_LOADING = "Tags.StartLoading";
	public static final String SUBJECT_TAGS_REFRESH = "Tags.Refresh";
	//public static final String SUBJECT_TAGS_DONE_LOADING = "Tags.DoneLoading";

	public static final String SUBJECT_FEEDS_START_LOADING = "Feeds.StartLoading";
	public static final String SUBJECT_FEEDS_REFRESH = "Feeds.Refresh";
	public static final String SUBJECT_FEEDS_DONE_LOADING = "Feeds.DoneLoading";

	public static final String SUBJECT_ARTICLE_START_LOADING = "Article.StartLoading";
	public static final String SUBJECT_ARTICLE_REFRESH = "Article.Refresh";
	public static final String SUBJECT_ARTICLE_DONE_LOADING = "Article.DoneLoading";

	public static final int EVENT_DELAYED = 200;
	public static final String ACTIONBAR_TITLE = "ActionBar.Title";

	/**
	 * we only update article content in cache if the new content length is at least
	 * around 90% of the existing content in cache
	 */
	public static final int ARTICLE_LENGTH_TOLERANT = 90;
}
