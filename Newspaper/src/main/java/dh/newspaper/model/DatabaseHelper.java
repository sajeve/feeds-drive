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
	public static final ReentrantLock WriteLock = new ReentrantLock();

	public DatabaseHelper(Context context) {
		super(context, Constants.DATABASE_NAME + ".db", RefData.getCachePath(context), null, Constants.DATABASE_VERSION);
	}

	/**
	 * all writing to database should use this method to be synchronized
	 */
	public void write(DatabaseWriting dbw) {
		WriteLock.lock();
		try {
			DaoMaster daoMaster = new DaoMaster(createWritableDatabase());
			try {
				DaoSession daoSession = daoMaster.newSession();
				dbw.doWrite(daoSession);
			} finally {
				daoMaster.getDatabase().close();
			}
		}
		finally {
			WriteLock.unlock();
		}
	}

	public static interface DatabaseWriting {
		public void doWrite(DaoSession daoSession);
	}
}
