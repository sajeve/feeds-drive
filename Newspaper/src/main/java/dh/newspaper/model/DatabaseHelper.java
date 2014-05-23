package dh.newspaper.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import dh.newspaper.Constants;

/**
 * Created by hiep on 22/05/2014.
 */
public class DatabaseHelper extends SQLiteAssetHelper
{
	public static final SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	private static final String DATABASE_NAME = Constants.DATABASE_NAME + ".db";

	// private LruCache<Long, BaseEntity> entityCache;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, getDatabaseDir(context), null, Constants.DATABASE_VERSION);
	}

	public static long getNow() {
		return Calendar.getInstance().getTime().getTime();
	}

	private static String getDatabaseDir(Context context) {
		if (Constants.DEBUG) {
			return "/mnt/shared/bridge";
		}
		else {
			return context.getExternalCacheDir().getAbsolutePath(); /* /storage/emulated/0/Android/data/dh.newspaper/cache */
			//context.getExternalFilesDir(null).getAbsolutePath()   /* /storage/emulated/0/Android/data/dh.newspaper/files */
		}
	}
}
