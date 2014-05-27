package dh.newspaper.base;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import dh.newspaper.MyApplication;
import dh.newspaper.model.DatabaseHelper;

/**
 * Created by hiep on 26/05/2014.
 */
public class DatabaseActivity extends Activity {
	private SQLiteDatabase mWDatabase;
	private SQLiteDatabase mRDatabase;

	public SQLiteDatabase getWDatabase() {
		if (mWDatabase == null) {
			((MyApplication) this.getApplication()).getDbHelper().getWritableDatabase();
		}
		return mWDatabase;
	}

	public SQLiteDatabase getRDatabase() {
		if (mWDatabase == null) {
			((MyApplication) this.getApplication()).getDbHelper().getReadableDatabase();
		}
		return mRDatabase;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWDatabase.close();
		mRDatabase.close();
	}
}
