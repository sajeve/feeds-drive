package dh.newspaper.tools;

import android.content.res.Resources;
import android.util.Log;
import com.google.common.base.Strings;
import com.joestelmach.natty.DateGroup;
import dh.newspaper.R;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by hiep on 14/05/2014.
 */
public class DateUtils {
	private static final String TAG = DateUtils.class.getName();
	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

	/**
	 * Warning: make sure that all items are not null
	 */
	private static final DateTimeParser[] jodaDateTimeParsers = {
			ISODateTimeFormat.dateTimeParser().getParser(), //2014-05-25T05:39:45Z same as "YYYY-MM-dd'T'HH:mm:ss'Z'"
			DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss Z").getParser(), //Mon, 26 May 2014 00:08:43 +0700
			DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss zzz").getParser(), //Sun, 25 May 2014 17:15:22 UTC
			DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss").getParser() //Fri, 15 Aug 2014 21:44:00
			//Sun, 25 May 2014 17:15:22 African
//			DateTimeFormat.forPattern("M/d/y K:m:ss a").getParser(), // 8/3/2014 4:37:00 AM
//			DateTimeFormat.forPattern("M-d-y K:m:ss a").getParser() // 8-3-2014 4:37:00 AM
	};
	private static final SimpleDateFormat[] javaDateTimeFormatter = new SimpleDateFormat[] {
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"), //Sun, 25 May 2014 14:09:29 EDT or Sun, 25 May 2014 17:15:22 GMT
			new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzzz")
	};
	private static final DateTimeFormatter jodaDateTimeFormatter = new DateTimeFormatterBuilder().append( null, jodaDateTimeParsers).toFormatter();

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

	public static String getTimeAgo(Resources resources, java.util.Date date) {
		if (date==null) {
			return "";
		}
		return getTimeAgo(resources, new DateTime(date));
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

	/**
	 * past = true,
	 */
	public static DateTime parseDateTime(String dateTimeStr) {
		if (Strings.isNullOrEmpty(dateTimeStr)) {
			return null;
		}
		DateTime resu = null;
		try {
			resu = jodaDateTimeFormatter.parseDateTime(dateTimeStr);
			if (resu!=null) {
				return resu;
			}
		}
		catch (IllegalArgumentException iae) {
			Log.w(TAG, "Failed joda DateTime parse '"+dateTimeStr+"': "+iae+". Try java DateTime parse");
		}

		for (SimpleDateFormat sdf : javaDateTimeFormatter) {
			try {
				Date d = sdf.parse(dateTimeStr);
				resu = new DateTime(d);
				if (resu!=null) {
					return resu;
				}
			}
			catch (ParseException pe) {
				Log.w(TAG, "Failed parse DateTime '"+dateTimeStr+"' (tried both java and joda parser). Add template in DateUtils.java. "+ pe);
			}
		}

		try {
			resu = parseNattyDate(dateTimeStr);
			if (resu != null) {
				return resu;
			}
		}
		catch (Exception ex) {
			Log.e(TAG, "Failed Natty parse DateTime '"+dateTimeStr+"'. "+ex);
		}

		try {
			resu = new DateTime(new org.pojava.datetime.DateTime(dateTimeStr).toDate());
			if (resu != null) {
				return resu;
			}
		}
		catch (Exception ex) {
			Log.w(TAG, "Failed PoJava parse DateTime '"+dateTimeStr+"'. "+ex);
		}

		return resu;
	}

	/**
	 * Published date must be in the past.
	 * reverse month and day 7/12 - 12/7 to have a reasonable publishing date
	 */
	public static DateTime parsePublishedDate(String dateTimeStr) {
		DateTime resu = parseDateTime(dateTimeStr);
		if (resu.isAfter(DateTime.now())) {
			//try to fix it by reverse month and day
			int day = resu.getDayOfMonth();
			int month = resu.getMonthOfYear();
			if (day<=12) {
				resu = new DateTime(resu.getYear(), day, month, resu.getHourOfDay(), resu.getMinuteOfHour(), resu.getSecondOfMinute(), resu.getMillisOfSecond(), resu.getChronology());
				if (resu.isBefore(DateTime.now())) {
					Log.d(TAG, "reverse day-month to obtain "+ SDF.format(resu.toDate()));
					return resu;
				}
			}
		}
		return null;
	}

	private static com.joestelmach.natty.Parser nattyParser = new com.joestelmach.natty.Parser();

	public static DateTime parseNattyDate(String dateTimeStr) {
		List<DateGroup> dateGroups = nattyParser.parse(dateTimeStr);
		if (dateGroups!=null && dateGroups.size()>0) {
			List<Date> dates = dateGroups.get(0).getDates();
			if (dates!=null && dates.size()>0) {
				java.util.Date date = dates.get(0);
				if (date!=null) {
					return new DateTime(date);
				}
			}
		}
		return null;
	}
}
