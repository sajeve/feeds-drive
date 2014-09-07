package dh.newspaper.model;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import dh.newspaper.Constants;
import dh.newspaper.cache.RefData;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by hiep on 22/05/2014.
 */
public class DatabaseHelper extends SQLiteAssetHelper
{
	public static final ReentrantLock DbLock = new ReentrantLock();

	public DatabaseHelper(Context context) {
		super(context, Constants.DATABASE_NAME + ".db", RefData.getCachePath(context), null, Constants.DATABASE_VERSION);
	}

	/**
	 * all access to database must to use this method to be synchronized
	 */
	public void operate(DatabaseOperation dbo) {
		DbLock.lock();
		try {
			if (Constants.SINGLE_DATABASE_CONNECTION) {
				dbo.doOperate(defaultDaoSession());
			}
			else {
				DaoMaster daoMaster = new DaoMaster(createWritableDatabase());
				try {
					DaoSession daoSession = daoMaster.newSession();
					dbo.doOperate(daoSession);
				} finally {
					daoMaster.getDatabase().close();
				}
			}
		}
		finally {
			DbLock.unlock();
		}
	}

	private volatile DaoMaster mDaoMaster;
	private volatile DaoSession mDaoSession;

	public synchronized DaoMaster defaultDaoMaster() {
		if (mDaoMaster == null) {
			mDaoMaster = new DaoMaster(getWritableDatabase());
		}
		return mDaoMaster;
	}

	public synchronized DaoSession defaultDaoSession() {
		if (mDaoSession == null) {
			mDaoSession = defaultDaoMaster().newSession();
		}
		return mDaoSession;
	}

	public static interface DatabaseOperation {
		public void doOperate(DaoSession daoSession);
	}
}
