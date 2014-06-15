package dh.newspaper.test.dagger;

import android.database.sqlite.SQLiteOpenHelper;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.google.common.base.Stopwatch;
import de.greenrobot.dao.query.LazyList;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.base.Injector;
import dh.newspaper.model.generated.*;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.parser.SubscriptionFactory;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 25/05/2014.
 */
public class GreenDaoTest extends ActivityInstrumentationTestCase2<MainActivity> {
	public static final String TAG = GreenDaoTest.class.getName();

	public GreenDaoTest() {
		super(MainActivity.class);
	}

	DaoMaster mDaoMaster;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		QueryBuilder.LOG_SQL = true;
		QueryBuilder.LOG_VALUES = true;

		SQLiteOpenHelper helper = new DaoMaster.DevOpenHelper(this.getActivity(), Constants.DEBUG_DATABASE_PATH +"/"+Constants.DATABASE_NAME+".db", null); //debug only (because drops all tables)
		mDaoMaster = new DaoMaster(helper.getReadableDatabase());
		assertNotNull(mDaoMaster.getDatabase());
	}

	@Override
	protected void tearDown() throws Exception {
		mDaoMaster.getDatabase().close();
	}

	public void testLazyLoadPerf() throws IOException, FeedParserException {
		DaoSession daoSession = mDaoMaster.newSession();

		Stopwatch sw = Stopwatch.createStarted();
		LazyList<Article> l1 = daoSession.getArticleDao().queryBuilder().listLazy();
		Log.i(TAG, "Lazy load - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		long size = l1.size();
		Log.i(TAG, "size() = "+ size +" - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		long count = daoSession.getArticleDao().queryBuilder().count();
		Log.i(TAG, "Count =  "+count+" - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		for (int i = 0; i<500; i++) {
			l1.get(i);
		}
		Log.i(TAG, "Load 500 first items - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		Log.i(TAG, "getLoadedCount() = "+l1.getLoadedCount()+" - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");
	}

	public void testNormalLoadPerf() throws IOException, FeedParserException {
		DaoSession daoSession = mDaoMaster.newSession();

		Stopwatch sw = Stopwatch.createStarted();
		List<Article> l1 = daoSession.getArticleDao().queryBuilder().list();
		Log.i(TAG, "Load - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		long count = daoSession.getArticleDao().queryBuilder().count();
		Log.i(TAG, "Count =  "+count+" - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		sw.reset().start();
		for (int i = 0; i<500; i++) {
			l1.get(i);
		}
		Log.i(TAG, "Load 500 first items - "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");
	}
}
