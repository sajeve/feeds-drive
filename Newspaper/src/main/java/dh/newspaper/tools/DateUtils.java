package dh.newspaper.tools;

import android.content.res.Resources;
import android.util.Log;
import com.google.common.base.Strings;
import dh.newspaper.R;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hiep on 14/05/2014.
 */
public class DateUtils {
	private static final String TAG = DateUtils.class.getName();
//	private final static int GLIMPSE_SIZE = 60;
//	private final static String NON_THIN = "[^iIl1\\.,']";
//	private final static char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final DateTimeParser[] jodaDateTimeParsers = {
			ISODateTimeFormat.dateTimeParser().getParser(), //2014-05-25T05:39:45Z same as "YYYY-MM-dd'T'HH:mm:ss'Z'"
			DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss Z").getParser(), //Mon, 26 May 2014 00:08:43 +0700
			DateTimeFormat.forPattern( "EEE, dd MMM YYYY HH:mm:ss zzz" ).getParser(), //Sun, 25 May 2014 17:15:22 UTC
			DateTimeFormat.forPattern( "EEE, dd MMM YYYY HH:mm:ss zzzz" ).getParser() //Sun, 25 May 2014 17:15:22 African
	};
	private static final SimpleDateFormat[] javaDateTimeFormatter = new SimpleDateFormat[] {
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"), //Sun, 25 May 2014 14:09:29 EDT or Sun, 25 May 2014 17:15:22 GMT
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzzz")
	};
	private static final DateTimeFormatter jodaDateTimeFormatter = new DateTimeFormatterBuilder().append( null, jodaDateTimeParsers).toFormatter();

//	private static int textWidth(String str) {
//		return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
//	}

//	private static String cut(String text, int max, String trail, boolean includeSize) {
//		if (text == null) {
//			return "null";
//		}
//
//		String suffix = trail;
//		if (includeSize) {
//			suffix = suffix + " (length="+text.length()+")";
//		}
//		int suffixLength = suffix.length();
//
//		if (textWidth(text) <= max)
//			return text;
//
//		// Start by chopping off at the word before max
//		// This is an over-approximation due to thin-characters...
//		int end = text.lastIndexOf(' ', max-suffixLength);
//
//		// Just one long word. Chop it off.
//		if (end == -1)
//			return text.substring(0, max-suffixLength) + suffix;
//
//		// Step forward as long as textWidth allows.
//		int newEnd = end;
//		do {
//			end = newEnd;
//			newEnd = text.indexOf(' ', end + 1);
//
//			// No more spaces.
//			if (newEnd == -1)
//				newEnd = text.length();
//
//		} while (textWidth(text.substring(0, newEnd) + suffix) < max);
//
//		return text.substring(0, end) + suffix;
//	}
//
//	/**
//	 * Use to log a long string, give a quick glimpse at the first 60 characters
//	 */
//	public static String glimpse(String text) {
//		if (text == null) {
//			return "null";
//		}
//		if (text.length()<GLIMPSE_SIZE) {
//			return text;
//		}
//		return text.substring(0, GLIMPSE_SIZE)+".. (length="+text.length()+")";
//	}
//
//	public static String ellipsize(String text, int max) {
//		return cut(text, max, "...", false);
//	}
//
//	public static String cut(String text, int max) {
//		return cut(text, max, "", false);
//	}
//
//	public static String bytesToHex(byte[] bytes) {
//		char[] hexChars = new char[bytes.length * 2];
//		for ( int j = 0; j < bytes.length; j++ ) {
//			int v = bytes[j] & 0xFF;
//			hexChars[j * 2] = HEX_CHARS[v >>> 4];
//			hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
//		}
//		return new String(hexChars);
//	}
//
//	public static String getChecksum(MessageDigest md, String s) throws UnsupportedEncodingException {
//		if (Strings.isNullOrEmpty(s)) {
//			return null;
//		}
//		return StrUtils.bytesToHex(md.digest(s.toUpperCase().replaceAll("\\s", "").getBytes("UTF-8")));
//	}

	public static String getTimeAgo(Resources resources, String dateTimeStr) {
		if (resources == null) {
			return dateTimeStr;
		}
		if (Strings.isNullOrEmpty(dateTimeStr)) {
			return resources.getString(R.string.PERIOD_UNKNOWN);
		}
		DateTime dateTime = parseDateTime(dateTimeStr);
		if (dateTime!=null) {
			return getTimeAgo(resources, dateTime);
		}
		else {
			return dateTimeStr;
		}
	}

	public static String getTimeAgo(Resources resources, DateTime date) {
		Period period = new Period(date, DateTime.now());

		int years = period.getYears();
		if (2<=years) {
			return String.format(resources.getString(R.string.PERIOD_YEAR_AGO), years);
		}
		if (1<=years) {
			return resources.getString(R.string.PERIOD_LAST_YEAR);
		}

		int months = period.getMonths();
		if (2<=months) {
			return String.format(resources.getString(R.string.PERIOD_MONTH_AGO), months);
		}
		if (1<=months) {
			return resources.getString(R.string.PERIOD_LAST_MONTH);
		}

		int weeks = period.getWeeks();
		if (2<=weeks) {
			return String.format(resources.getString(R.string.PERIOD_WEEK_AGO), weeks);
		}
		if (1<=weeks) {
			return resources.getString(R.string.PERIOD_LAST_WEEK);
		}

		int days = period.getDays();
		if (2<=days) {
			return String.format(resources.getString(R.string.PERIOD_DAY_AGO), days);
		}
		if (1<=days) {
			return resources.getString(R.string.PERIOD_YESTERDAY);
		}

		int hours = period.getHours();
		if (1<=hours) {
			return String.format(resources.getString(R.string.PERIOD_HOURS_AGO), hours);
		}

		int minutes = period.getMinutes();
		if (1<=minutes) {
			return String.format(resources.getString(R.string.PERIOD_MINUTE_AGO), minutes);
		}

		return resources.getString(R.string.PERIOD_JUST_NOW);
	}

	public static DateTime parseDateTime(String dateTimeStr) {
		if (Strings.isNullOrEmpty(dateTimeStr)) {
			return null;
		}
		DateTime resu = null;
		try {
			resu = jodaDateTimeFormatter.parseDateTime(dateTimeStr);
		}
		catch (IllegalArgumentException iae) {
			Log.w(TAG, "Failed joda DateTime parse '"+dateTimeStr+"': "+iae.getMessage()+". Try java DateTime parse");
			for (SimpleDateFormat sdf : javaDateTimeFormatter) {
				try {
					Date d = sdf.parse(dateTimeStr);
					resu = new DateTime(d);
				}
				catch (ParseException pe) {
					Log.w(TAG, "Failed parse DateTime '"+dateTimeStr+"' (tried both java and joda parser). Add template in StrUtils.java");
				}
			}
		}

		return resu;
	}

//	/**
//	 * Remove double white space, and convert to upper case
//	 */
//	public static String normalizeUpper(String s) {
//		return s.trim().replaceAll("\\s+", " ").toUpperCase();
//	}

//	/**
//	 * Same as {@link java.lang.String#equalsIgnoreCase(String)}
//	 * prevent NullPointerException, return true if both a and b is Null or Empty
//	 */
//	public static boolean equalsIgnoreCases(String a, String b) {
//		if (Strings.isNullOrEmpty(a)) {
//			return Strings.isNullOrEmpty(b);
//		}
//		return a.equalsIgnoreCase(b);
//	}
//
//	/**
//	 * Same as {@link java.lang.String#equals(Object)}
//	 * prevent NullPointerException, return true if both a and b is Null or Empty
//	 */
//	public static boolean equalsString(String a, String b) {
//		if (Strings.isNullOrEmpty(a)) {
//			return Strings.isNullOrEmpty(b);
//		}
//		return a.equals(b);
//	}
//
//	/**
//	 * return true if str is shorter than percent of ref
//	 */
//	public static boolean tooShort(String str, String ref, int percent) {
//		if (percent < 0) {
//			return false;
//		}
//		int lenStr = Strings.isNullOrEmpty(str) ? 0 : str.length();
//		int lenRef =  Strings.isNullOrEmpty(ref) ? 0 : ref.length();
//		int shortestLenAllowed = lenRef*percent/100;
//		return lenStr < shortestLenAllowed;
//	}
}
