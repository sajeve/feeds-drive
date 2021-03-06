package dh.newspaper.test;

import android.database.sqlite.SQLiteOpenHelper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import de.greenrobot.dao.query.LazyList;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.DatabaseHelper;
import dh.newspaper.model.generated.*;
import dh.newspaper.modules.AppContextModule;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.parser.SubscriptionFactory;
import dh.newspaper.tools.DateUtils;
import dh.tool.common.PerfWatcher;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/**
 * Created by hiep on 25/05/2014.
 */
public class DatabaseGenTest extends ActivityInstrumentationTestCase2<MainActivity> {
	public DatabaseGenTest() {
		super(MainActivity.class);
	}

	DaoMaster mDaoMaster;

	RefData refData;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		QueryBuilder.LOG_SQL = true;
		QueryBuilder.LOG_VALUES = true;

		/*SQLiteOpenHelper helper = new DaoMaster.DevOpenHelper(this.getActivity(), "/mnt/shared/bridge/test/"+Constants.DATABASE_NAME+".db", null); //debug only (because drops all tables)
		mDaoMaster = new DaoMaster(helper.getReadableDatabase());
		assertNotNull(mDaoMaster.getDatabase());*/
	}

	@Override
	protected void tearDown() throws Exception {
		mDaoMaster.getDatabase().close();
		super.tearDown();
	}

	public void testGenerateDatabase() throws IOException, FeedParserException {
		Date now = DateTime.now().toDate();

		mDaoMaster.dropAllTables(mDaoMaster.getDatabase(), false);
		mDaoMaster.createAllTables(mDaoMaster.getDatabase(), false);
		DaoSession daoSession = mDaoMaster.newSession();

//		daoSession.getPathToContentDao().insert(new PathToContent(null,".+vnexpress.net/.+", "div.short_intro, div.relative_new, div.fck_detail", "vn", 0, true, now));
//		daoSession.getPathToContentDao().insert(new PathToContent(null,".+nytimes.com/.+", "div.article-body", "en-US", 0, true, now));
//		daoSession.getPathToContentDao().insert(new PathToContent(null,".+huffingtonpost.com/.+", "div.article>p", "en-US", 0, true, now));
//		daoSession.getPathToContentDao().insert(new PathToContent(null,".+cnn.com/.+", "div.articleContent", "en-US", 0, true, now));
//		daoSession.getPathToContentDao().insert(new PathToContent(null,".+vnexpress.net/interactive/.+", "div.block_main_menu", "vn", 1, true, now));

		SubscriptionFactory subFactory = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(SubscriptionFactory.class);

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/tin-moi-nhat.rss", new String[] {"Thời sự"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/doi-song.rss", new String[] {"Đời sống"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/the-gioi.rss", new String[] {"Thế giới"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://rss.nytimes.com/services/xml/rss/nyt/InternationalHome.xml", new String[] {"Thế giới"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://rss.nytimes.com/services/xml/rss/nyt/Europe.xml", new String[] {"World"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Business", new String[] {"Business"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.huffingtonpost.com/feeds/verticals/business/index.xml", new String[] {"Business"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Technology", new String[] {"Technology"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.huffingtonpost.com/feeds/verticals/technology/index.xml", new String[] {"Technology"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.huffingtonpost.com/tag/asian-americans/feed", new String[] {"Asian Americans"}));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://rss.cnn.com/rss/edition.rss", new String[]{"Top Stories"}));
	}

	public void testQueryDatabase() {
		String tag="Business";

		// Query to find all subscription with tag Business
		DaoSession daoSession = mDaoMaster.newSession();
		QueryBuilder<Subscription> subscriptionByTagQueryBuilder = daoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Tags.like("%|" + tag.toUpperCase() + "|%")
						, SubscriptionDao.Properties.Enable.eq(Boolean.TRUE));
		Query<Subscription> subscriptionByTagQuery = subscriptionByTagQueryBuilder.build();

		{
			LazyList<Subscription> subscriptions = subscriptionByTagQuery.listLazy();
			for (Subscription sub : subscriptions) {
				assertTrue(sub.getTags().contains("|" + tag.toUpperCase() + "|"));
				System.out.println(sub.getFeedsUrl());
			}
			subscriptions.close();
		}

		String tag2 = "Technology";
		subscriptionByTagQuery.setParameter(0, tag2);
		{
			LazyList<Subscription> subscriptions = subscriptionByTagQuery.listLazy();
			for (Subscription sub : subscriptions) {
				assertTrue(sub.getTags().contains("|" + tag2.toUpperCase() + "|"));
				System.out.println(sub.getFeedsUrl());
			}
			subscriptions.close();
		}

		/*
		//To delete query
		subscriptionByTagQueryBuilder.buildDelete().executeDeleteWithoutDetachingEntities();
		daoSession.clear();
		*/
	}

	public void testUpdateDatabase() {
		String tag="Business";

		DaoSession daoSession = mDaoMaster.newSession();

		// before update: find all subscription with tag = "Business"
		QueryBuilder<Subscription> subscriptionByTagQueryBuilder = daoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Tags.like("%|" + tag.toUpperCase() + "|%")
						, SubscriptionDao.Properties.Enable.eq(Boolean.TRUE));
		Query<Subscription> subscriptionByTagQuery = subscriptionByTagQueryBuilder.build();

		Subscription firstSubscriptionFound = subscriptionByTagQuery.list().get(0);
		assertTrue(firstSubscriptionFound.getEnable());

		//update 1: disable all subscription with tag Business

		String updateQuery = "update "+SubscriptionDao.TABLENAME
				+ " set "+SubscriptionDao.Properties.Enable.columnName + "=?"
				+" where " + SubscriptionDao.Properties.Tags.columnName + " like ?";

		mDaoMaster.getDatabase().execSQL(updateQuery, new Object[] {Boolean.FALSE, "%|" + tag.toUpperCase() + "|%"});

		//after update: we still got the same value in cache
		assertTrue(firstSubscriptionFound.getEnable());

		//now we'll update the entity in cache:
		daoSession.getSubscriptionDao().refresh(firstSubscriptionFound);
		assertFalse(firstSubscriptionFound.getEnable());

		//update 2: disable the subscription again! this time, with GreenDAO (not with the native query)
		firstSubscriptionFound.setEnable(true);
		daoSession.update(firstSubscriptionFound);
		daoSession.getSubscriptionDao().refresh(firstSubscriptionFound);
		assertTrue(firstSubscriptionFound.getEnable());
	}

	private static final Logger log = LoggerFactory.getLogger(DatabaseGenTest.class);

	public void testFixPublishedDate() {
		AppContextModule appContextModule = new AppContextModule(this.getActivity().getApplicationContext());
		final DaoSession daoSession = appContextModule.provideDatabaseHelper().defaultDaoSession();

		daoSession.runInTx(new Runnable() {
			@Override
			public void run() {
				long count = daoSession.getArticleDao().count();
				int fixed = 0;
				for (int i=0; i<count; i++) {
					Article mArticle = daoSession.getArticleDao().loadByRowId(i);
					if (mArticle == null) {
						continue;
					}
					PerfWatcher pw = new PerfWatcher(log, mArticle.getArticleUrl());

					Date minDate = mArticle.getLastUpdated();
					if (mArticle.getLastDownloadSuccess() != null && minDate.after(mArticle.getLastDownloadSuccess())) {
						minDate = mArticle.getLastDownloadSuccess();
					}
					if (mArticle.getPublishedDate().after(minDate)) {
						//Fix: the published date must be anterior to minDate.
						DateTime publishedDate = DateUtils.parsePublishedDate(mArticle.getPublishedDateString());
						if (publishedDate == null || publishedDate.toDate().after(minDate)) {
							mArticle.setPublishedDate(minDate);
							pw.i("Fix published date to minDate: " + DateUtils.SDF.format(mArticle.getPublishedDate()));
						}
						else {
							mArticle.setPublishedDate(publishedDate.toDate());
							pw.i("Fix published date: " + DateUtils.SDF.format(mArticle.getPublishedDate()));
						}
						daoSession.getArticleDao().update(mArticle);
						fixed++;
					}
				}
				Log.i("FixPublishedDate", fixed+"/"+count+" fixed");
			}
		});
	}

/*	public void testLazyLoadPerf() {
		DaoSession daoSession = mDaoMaster.newSession();
		m

		//daoSession.getArticleDao().
	}*/
}
