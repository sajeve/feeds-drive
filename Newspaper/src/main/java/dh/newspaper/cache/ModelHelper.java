//package dh.newspaper.cache;
//
//import android.database.Cursor;
//import android.util.Log;
//import com.google.common.base.Splitter;
//import com.google.common.base.Stopwatch;
//import com.google.common.base.Strings;
//import de.greenrobot.dao.query.Query;
//import de.greenrobot.dao.query.QueryBuilder;
//import de.greenrobot.dao.query.WhereCondition;
//import dh.newspaper.Constants;
//import dh.newspaper.model.FeedItem;
//import dh.newspaper.model.generated.*;
//import dh.newspaper.parser.ContentParser;
//import dh.newspaper.tools.StrUtils;
//import org.joda.time.DateTime;
//import org.joda.time.Duration;
//
//import javax.inject.Inject;
//import java.io.IOException;
//import java.security.MessageDigest;
//import java.util.Date;
//import java.util.List;
//import java.util.TreeSet;
//import java.util.concurrent.TimeUnit;
//
///**
// * Helper to download and cache article to database
// * Created by hiep on 31/05/2014.
// */
//public class ModelHelper {
//	private static final String TAG = ModelHelper.class.getName();
//
//	private QueryBuilder<Article> mFindArticleByUrlQueryBuilder;
//	private Query<Subscription> mFindSubscriptionByTagQuery;
//
//	private DaoSession mDaoSession;
//	private ContentParser mContentParser;
//	private MessageDigest mMessageDigest;
//	private List<PathToContent> mPathToContents;
//
//	@Inject
//	public ModelHelper(DaoSession daoSession, ContentParser contentParser, MessageDigest messageDigest) {
//		mDaoSession = daoSession;
//		mContentParser = contentParser;
//		init();
//	}
//
//	/**
//	 * Init queryBuilder and cache some object
//	 */
//	private void init() {
//		mFindArticleByUrlQueryBuilder = mDaoSession.getArticleDao().queryBuilder()
//				.where(ArticleDao.Properties.ArticleUrl.eq(""));
//		mFindSubscriptionByTagQuery = mDaoSession.getSubscriptionDao().queryBuilder()
//				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE),
//						SubscriptionDao.Properties.Tags.like("Sample Category"))
//				.build();
//	}
//
//	/**
//	 * Search feedItem in the database, download it if not found.
//	 * force update the cache if the cached article is expiry: lastUpdate older than ttl (default = 1h)
//	 * otherwise returns immediately the cached article
//	 */
//	public Article get(FeedItem feedItem, Duration ttl, SelectSingleArticleCallback callback) {
//		String feedItemUri = feedItem.getUri();
//
//		//query the cache
//		Stopwatch sw = logFirstStep("Get Article()", feedItemUri);
//
//		Query<Article> query = mFindArticleByUrlQueryBuilder.build();
//		query.setParameter(0, feedItemUri);
//		Article article = query.unique();
//		log("Find article in cache returned "+article, sw, feedItemUri);
//
//		if (article != null) {
//			if (callback != null) {
//				callback.onFinishedLoadFromCache(article);
//			}
//
//			boolean articleIsExpiry = (ttl == null) || new Duration(new DateTime(article.getLastUpdated()), DateTime.now()).isLongerThan(ttl);
//
//			if (!articleIsExpiry) {
//				if (callback != null) {
//					callback.done(article);
//				}
//				return article;
//			}
//
//			log("Article in cache was expiry: ttl="+ttl.getMillis()+" lastUpdated="+article.getLastUpdated(), sw, feedItemUri);
//		}
//
//		//refresh cache then return
//		log("Start Refresh cache", sw, feedItemUri);
//
//		String articleContent = null;
//
//		PathToContent pathToContent = findFirstMatchingPathToContent(feedItemUri);
//		log("Find matched xpath returned "+pathToContent, sw, feedItemUri);
//
//		String parseNotice = null;
//		if (pathToContent == null) {
//			articleContent = feedItem.getDescription();
//			parseNotice = "[0] Path not found in "+PathToContentDao.TABLENAME;
//		}
//		else {
//			try {
//				articleContent = mContentParser.extractContent(feedItemUri, pathToContent.getXpath()).html();
//				if (Strings.isNullOrEmpty(articleContent)) {
//					articleContent = feedItem.getDescription();
//					parseNotice = "[1] Empty content";
//				}
//			} catch (IOException e) {
//				articleContent = feedItem.getDescription();
//				parseNotice = "[2] IOException: "+e.getMessage();
//			} catch (Exception e) {
//				articleContent = feedItem.getDescription();
//				parseNotice = "[3] Exception: "+e.getMessage();
//			}
//
//		}
//		log("Get article content returned: "+StrUtils.glimpse(articleContent), sw, feedItemUri);
//
//		String articleLanguage = feedItem.getLanguage() == null ? pathToContent.getLanguage() : feedItem.getLanguage();
//
//		Date now = DateTime.now().toDate();
//		//Date now = Calendar.getInstance().getTime().getTime()
//
//		if (article == null) {
//			if (!Strings.isNullOrEmpty(parseNotice)) {
//				parseNotice += ". Use feed description as content";
//			}
//
//			//not found -> insert new
//			article = new Article(null, //id
//					feedItemUri,
//					feedItem.getParentUrl(),
//					feedItem.getImageUrl(),
//					feedItem.getTitle(),
//					feedItem.getAuthor(),
//					feedItem.getExcerpt(),
//					articleContent,
//					null, //checksum
//					articleLanguage,
//					0L, //openedCount
//					feedItem.getPublishedDate(),
//					StrUtils.parseDateTime(feedItem.getPublishedDate()).toDate(),
//					null,//date archive
//					null,//last open
//					now, //last updated
//					pathToContent == null ? null : pathToContent.getXpath(), //xpath
//					parseNotice
//			);
//
//			mDaoSession.getArticleDao().insert(article);
//			log("Inserted new: "+article, sw, feedItemUri);
//		}
//		else {
//			boolean shouldUpdateContent = false;
//
//			if (Strings.isNullOrEmpty(parseNotice)) {
//				shouldUpdateContent = true;
//			}
//			else {
//				if (Strings.isNullOrEmpty(article.getContent())) {
//					parseNotice += ". And old cached content is empty. Use feed description as content";
//					shouldUpdateContent = true;
//				}
//				else {
//					parseNotice += ". Keep old content existed in cache";
//				}
//			}
//
//			if (shouldUpdateContent) {
//				article.setContent(articleContent);
//				article.setLastUpdated(now);
//				article.setLanguage(articleLanguage);
//			}
//			article.setParseNotice(parseNotice);
//
//			mDaoSession.getArticleDao().update(article);
//			log("Updated: " + article, sw, feedItemUri);
//		}
//
//		if (callback != null) {
//			callback.onFinishedUpdateCache(article);
//			callback.done(article);
//		}
//		return article;
//	}
//	public Article get(FeedItem feedItem, SelectSingleArticleCallback callback) {
//		return get(feedItem, Constants.ARTICLE_TTL, callback);
//	}
//
//	public PathToContent findFirstMatchingPathToContent(String articleUrl) {
//		for(PathToContent ptc : pathToContentList()) {
//			if (articleUrl.matches(ptc.getUrlPattern())) {
//				return ptc;
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Return list of enabled PathToContent order by priority
//	 */
//	private synchronized List<PathToContent> pathToContentList() {
//		if (mPathToContents==null) {
//			mPathToContents = mDaoSession.getPathToContentDao().queryBuilder()
//					.where(PathToContentDao.Properties.Enable.eq(Boolean.TRUE))
//					.orderDesc(PathToContentDao.Properties.Priority)
//					.list();
//		}
//		return mPathToContents;
//	}
//
//	/**
//	 * Get all possible tags from active subscription (alphabetic order)
//	 */
//	public TreeSet<String> getActiveTags() {
//		Stopwatch sw = logFirstStep("GetTags()");
//
//		List<Subscription> subscriptions = mDaoSession.getSubscriptionDao().queryBuilder()
//				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE))
//				.list();
//		log("Found "+subscriptions.size()+" active subscriptions", sw);
//
//		TreeSet<String> activeTags = new TreeSet<String>();
//		for (Subscription sub : subscriptions) {
//			Iterable<String> subTags = Splitter.on('|').omitEmptyStrings().split(sub.getActiveTags());
//			for (String tag : subTags) {
//				activeTags.add(tag);
//			}
//		}
//		log("Found "+activeTags.size()+" tags from "+subscriptions.size()+" active subscription tags", sw);
//
//		return  activeTags;
//	}
//
//	/**
//	 * Find subscriptions by tag: Select * from Subscription where tag like "%|tag|%"
//	 * @param tag
//	 */
//	public List<Subscription> getActiveSubscriptions(String tag) {
//		mFindSubscriptionByTagQuery.setParameter(1, "%"+getTechnicalTag(tag)+"%");
//		Stopwatch sw = logFirstStep("getFeedsUrl("+tag+")");
//		List<Subscription> subscriptions = mFindSubscriptionByTagQuery.list();
//		log("Found "+subscriptions.size()+" active subscriptions for tag '"+tag+"'", sw);
//		return subscriptions;
//	}
//
//	/**
//	 * Get Feeds from tag and update cache. Might be 10000 article or more
//	 * @param tag
//	 * @return
//	 */
////	public Cursor getArticles(String tag, SelectTagCallback callback) {
////
////		getArticles
////
////		//get all article from cache
////
////		List<Subscription> subscriptions = getActiveSubscriptions(tag);
////		if (subscriptions.isEmpty()) {
////			return null;
////		}
////
////		QueryBuilder<Article> queryBuilder = mDaoSession.getArticleDao().queryBuilder();
////		int subCount = subscriptions.size();
////		if (subCount == 1) {
////			queryBuilder.where(ArticleDao.Properties.ParentUrl.eq(subscriptions.get(0).getFeedsUrl()));
////		}
////		else if (subCount == 2) {
////			queryBuilder.whereOr(
////					ArticleDao.Properties.ParentUrl.eq(subscriptions.get(0).getFeedsUrl()),
////					ArticleDao.Properties.ParentUrl.eq(subscriptions.get(1).getFeedsUrl())
////			);
////		}
////		else { //subCount > 2
////			WhereCondition[] whereConditions = new WhereCondition[subCount-2];
////			for (int i = 2; i<subCount; i++) {
////				whereConditions[i-2] = ArticleDao.Properties.ParentUrl.eq(subscriptions.get(i).getFeedsUrl());
////			}
////
////			queryBuilder.whereOr(
////					ArticleDao.Properties.ParentUrl.eq(subscriptions.get(0).getFeedsUrl()),
////					ArticleDao.Properties.ParentUrl.eq(subscriptions.get(1).getFeedsUrl()),
////					whereConditions
////			);
////		}
////
////
////	}
//
//	private String getTechnicalTag(String tag) {
//		return "|"+StrUtils.normalizeUpper(tag)+"|";
//	}
//
//	/**
//	 * Simple work-flow steps logger, reset stopwatch every step
//	 */
//	private Stopwatch logFirstStep(String message, String id) {
//		Log.i(TAG ,message +" - " + id);
//		return Stopwatch.createStarted();
//	}
//	private void log(String message, Stopwatch sw, String id) {
//		Log.d(TAG, message + " ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) - " + id);
//		sw.reset().start();
//	}
//	private Stopwatch logFirstStep(String message) {
//		Log.i(TAG, message);
//		return Stopwatch.createStarted();
//	}
//	private void log(String message, Stopwatch sw) {
//		Log.d(TAG, message + " (" + sw.elapsed(TimeUnit.MILLISECONDS) + " ms)");
//		sw.reset().start();
//	}
//
//	public static interface SelectSingleArticleCallback {
//		public void onFinishedLoadFromCache(Article article);
//		public void onFinishedUpdateCache(Article article);
//		public void done(Article article);
//	}
//	public static interface SelectTagCallback {
//		public void onFinishedLoadFromCache(Article article);
//		public void onFinishedUpdateCache(Article article);
//		public void done(Article article);
//	}
//}
