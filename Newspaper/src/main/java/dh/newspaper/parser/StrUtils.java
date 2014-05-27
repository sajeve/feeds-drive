package dh.newspaper.parser;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import com.google.common.base.Strings;
import dh.newspaper.R;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hiep on 14/05/2014.
 */
public class StrUtils {
	private static final String TAG = StrUtils.class.getName();

	private final static String NON_THIN = "[^iIl1\\.,']";
	private final static char[] HEX_CHARS = "0123456789abcdef".toCharArray();
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

	private static int textWidth(String str) {
		return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
	}

	public static String ellipsize(String text, int max) {
		if (textWidth(text) <= max)
			return text;

		// Start by chopping off at the word before max
		// This is an over-approximation due to thin-characters...
		int end = text.lastIndexOf(' ', max - 3);

		// Just one long word. Chop it off.
		if (end == -1)
			return text.substring(0, max-3) + "...";

		// Step forward as long as textWidth allows.
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = text.indexOf(' ', end + 1);

			// No more spaces.
			if (newEnd == -1)
				newEnd = text.length();

		} while (textWidth(text.substring(0, newEnd) + "...") < max);

		return text.substring(0, end) + "...";
	}

	public static String cut(String text, int max) {

		if (textWidth(text) <= max)
			return text;

		// Start by chopping off at the word before max
		// This is an over-approximation due to thin-characters...
		int end = text.lastIndexOf(' ', max);

		// Just one long word. Chop it off.
		if (end == -1)
			return text.substring(0, max);

		// Step forward as long as textWidth allows.
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = text.indexOf(' ', end + 1);

			// No more spaces.
			if (newEnd == -1)
				newEnd = text.length();

		} while (textWidth(text.substring(0, newEnd)) < max);

		return text.substring(0, end);
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_CHARS[v >>> 4];
			hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String getChecksum(MessageDigest md, String s) throws UnsupportedEncodingException {
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		return StrUtils.bytesToHex(md.digest(s.toUpperCase().replaceAll("\\s", "").getBytes("UTF-8")));
	}

	public static String getTimeAgo(Context context, String dateTimeStr) {
		DateTime dateTime = parseDateTime(dateTimeStr);
		if (dateTime!=null) {
			return getTimeAgo(context, dateTime);
		}
		else {
			return dateTimeStr;
		}
	}

	public static String getTimeAgo(Context context, DateTime date) {
		Period period = new Period(date, DateTime.now());

		int years = period.getYears();
		if (2<=years) {
			return String.format(context.getResources().getString(R.string.PERIOD_YEAR_AGO), years);
		}
		if (1<=years) {
			return context.getResources().getString(R.string.PERIOD_LAST_YEAR);
		}

		int months = period.getMonths();
		if (2<=months) {
			return String.format(context.getResources().getString(R.string.PERIOD_MONTH_AGO), months);
		}
		if (1<=months) {
			return context.getResources().getString(R.string.PERIOD_LAST_MONTH);
		}

		int weeks = period.getWeeks();
		if (2<=weeks) {
			return String.format(context.getResources().getString(R.string.PERIOD_WEEK_AGO), weeks);
		}
		if (1<=weeks) {
			return context.getResources().getString(R.string.PERIOD_LAST_WEEK);
		}

		int days = period.getDays();
		if (2<=days) {
			return String.format(context.getResources().getString(R.string.PERIOD_DAY_AGO), days);
		}
		if (1<=days) {
			return context.getResources().getString(R.string.PERIOD_YESTERDAY);
		}

		int hours = period.getHours();
		if (1<=hours) {
			return String.format(context.getResources().getString(R.string.PERIOD_HOURS_AGO), hours);
		}

		int minutes = period.getMinutes();
		if (1<=minutes) {
			return String.format(context.getResources().getString(R.string.PERIOD_MINUTE_AGO), minutes);
		}

		return context.getResources().getString(R.string.PERIOD_JUST_NOW);
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
}
