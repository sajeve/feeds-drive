package dh.newspaper.test;

import android.database.sqlite.SQLiteOpenHelper;
import android.test.ActivityInstrumentationTestCase2;
import de.greenrobot.dao.query.LazyList;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.parser.SubscriptionFactory;
import org.joda.time.DateTime;

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

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		QueryBuilder.LOG_SQL = true;
		QueryBuilder.LOG_VALUES = true;

		SQLiteOpenHelper helper = new DaoMaster.DevOpenHelper(this.getActivity(), "/mnt/shared/bridge/test/"+Constants.DATABASE_NAME+".db", null); //debug only (because drops all tables)
		mDaoMaster = new DaoMaster(helper.getReadableDatabase());
		assertNotNull(mDaoMaster.getDatabase());
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

		daoSession.getPathToContentDao().insert(new PathToContent(null,".+vnexpress.net/.+", "div.short_intro, div.relative_new, div.fck_detail", "vn", 0, true, now));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+nytimes.com/.+", "div.article-body", "en-US", 0, true, now));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+huffingtonpost.com/.+", "div.article>p", "en-US", 0, true, now));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+cnn.com/.+", "div.articleContent", "en-US", 0, true, now));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+vnexpress.net/interactive/.+", "div.block_main_menu", "vn", 1, true, now));

		SubscriptionFactory subFactory = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(SubscriptionFactory.class);

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/tin-moi-nhat.rss", new String[] {"Thời sự"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/doi-song.rss", new String[] {"Đời sống"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/the-gioi.rss", new String[] {"Thế giới"}, "vn"));

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/dh.newspaper.services/xml/rss/nyt/HomePage.xml", new String[] {"Home US"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/dh.newspaper.services/xml/rss/nyt/InternationalHome.xml", new String[] {"Home World"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/dh.newspaper.services/xml/rss/nyt/World.xml", new String[] {"World"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Business", new String[] {"Business"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Technology", new String[] {"Technology"}, "vn"));

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.huffingtonpost.com/tag/asian-americans/feed", new String[] {"Asian Americans"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://rss.cnn.com/rss/edition.rss", new String[]{"Top Stories"}, "vn"));
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
}
