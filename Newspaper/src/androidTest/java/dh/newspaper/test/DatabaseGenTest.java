package dh.newspaper.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.ActivityInstrumentationTestCase2;
import de.greenrobot.dao.query.QueryBuilder;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.parser.StrUtils;
import dh.newspaper.parser.SubscriptionFactory;
import org.joda.time.DateTime;

import java.io.IOException;

/**
 * Created by hiep on 25/05/2014.
 */
public class DatabaseGenTest extends ActivityInstrumentationTestCase2<MainActivity> {
	public DatabaseGenTest() {
		super(MainActivity.class);
	}

	public void testGenerateDatabase() throws IOException, FeedParserException {
		QueryBuilder.LOG_SQL = true;
		QueryBuilder.LOG_VALUES = true;

		SQLiteOpenHelper helper = new DaoMaster.DevOpenHelper(this.getActivity(), "/mnt/shared/bridge/test/"+Constants.DATABASE_NAME+".db", null); //debug only (because drops all tables)
		SQLiteDatabase db = helper.getWritableDatabase();

		DaoMaster daoMaster = new DaoMaster(db);
		DaoMaster.dropAllTables(db, false);
		DaoMaster.createAllTables(db, false);

		DaoSession daoSession = daoMaster.newSession();
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+vnexpress.net/.+", "div.short_intro, div.relative_new, div.fck_detail", "vn", true, DateTime.now().toDate()));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+nytimes.com/.+", "div.article-body", "en-US", true, DateTime.now().toDate()));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+huffingtonpost.com/.+", "div.article>p", "en-US", true, DateTime.now().toDate()));
		daoSession.getPathToContentDao().insert(new PathToContent(null,".+cnn.com/.+", "div.articleContent", "en-US", true, DateTime.now().toDate()));

		SubscriptionFactory subFactory = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(SubscriptionFactory.class);

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/tin-moi-nhat.rss", new String[] {"Thời sự"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/doi-song.rss", new String[] {"Đời sống"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://vnexpress.net/rss/the-gioi.rss", new String[] {"Thế giới"}, "vn"));

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/services/xml/rss/nyt/HomePage.xml", new String[] {"Home US"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/services/xml/rss/nyt/InternationalHome.xml", new String[] {"Home World"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.nytimes.com/services/xml/rss/nyt/World.xml", new String[] {"World"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Business", new String[] {"Business"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://feeds.nytimes.com/nyt/rss/Technology", new String[] {"Technology"}, "vn"));

		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://www.huffingtonpost.com/tag/asian-americans/feed", new String[] {"Asian Americans"}, "vn"));
		daoSession.getSubscriptionDao().insert(subFactory.createSubscription("http://rss.cnn.com/rss/edition.rss", new String[]{"Top Stories"}, "vn"));

		db.close();
	}

}
