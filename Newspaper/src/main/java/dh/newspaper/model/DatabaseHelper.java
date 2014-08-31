package dh.newspaper.model;

import android.content.Context;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import dh.newspaper.Constants;
import dh.newspaper.cache.RefData;

/**
 * Created by hiep on 22/05/2014.
 */
public class DatabaseHelper extends SQLiteAssetHelper
{
	public DatabaseHelper(Context context) {
		super(context, Constants.DATABASE_NAME + ".db", RefData.getCachePath(context), null, Constants.DATABASE_VERSION);
	}
}
