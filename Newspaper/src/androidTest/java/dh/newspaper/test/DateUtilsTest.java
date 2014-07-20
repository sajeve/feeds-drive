package dh.newspaper.test;

import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import dh.newspaper.MainActivity;
import dh.newspaper.tools.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hiep on 25/05/2014.
 */
public class DateUtilsTest extends ActivityInstrumentationTestCase2<MainActivity> {
	public DateUtilsTest() {
		super(MainActivity.class);
	}

	public void testTimeAgo() {
		DateTime now = DateTime.now();

		Resources resources = this.getActivity().getResources();
		
		DateTime seconds2ago = now.minusSeconds(1);
		assertEquals("Just now", DateUtils.getTimeAgo(resources, seconds2ago));

		DateTime minutes2ago = now.minusMinutes(2);
		assertEquals("2 minutes ago", DateUtils.getTimeAgo(resources, minutes2ago));

		DateTime hours2ago = now.minusHours(2);
		assertEquals("2 hours ago", DateUtils.getTimeAgo(resources, hours2ago));

		DateTime yesterday = now.minusDays(1);
		assertEquals("Yesterday", DateUtils.getTimeAgo(resources, yesterday));
		DateTime days2ago = now.minusDays(2);
		assertEquals("2 days ago", DateUtils.getTimeAgo(resources, days2ago));

		DateTime lastWeek = now.minusWeeks(1);
		assertEquals("Last week", DateUtils.getTimeAgo(resources, lastWeek));
		DateTime weeks2ago = now.minusWeeks(2);
		assertEquals("2 weeks ago", DateUtils.getTimeAgo(resources, weeks2ago));

		DateTime lastMonth = now.minusMonths(1);
		assertEquals("Last month", DateUtils.getTimeAgo(resources, lastMonth));
		DateTime months2ago = now.minusMonths(2);
		assertEquals("2 months ago", DateUtils.getTimeAgo(resources, months2ago));

		DateTime lastYear = now.minusYears(1);
		assertEquals("Last year", DateUtils.getTimeAgo(resources, lastYear));
		DateTime years2ago = now.minusYears(2);
		assertEquals("2 years ago", DateUtils.getTimeAgo(resources, years2ago));
	}

	public void testParseDateTimeClassic() throws ParseException {
		/*{
			SimpleDateFormat parserSDF = new SimpleDateFormat("EEE, dd MMM YYYY HH:mm:ss Z");
			Date d = parserSDF.parse("Mon, 26 May 2014 00:08:43 +0700");
		}*/
		{
			SimpleDateFormat parserSDF = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			Date cd = parserSDF.parse("Sun, 25 May 2014 14:09:29 EDT");
			DateTime d = new DateTime(cd);
			assertEquals(2014, d);
		}
	}

	public void testParseDateTime() {
		Log.i("StrUtilsTest", Arrays.toString(DateTimeZone.getAvailableIDs().toArray()));

		{
			DateTime d = DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss zzz").parseDateTime("Sun, 25 May 2014 14:09:29 EDT");
			assertEquals(2014, d.getYear());
		}
		{
			DateTime d = DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss zzz").parseDateTime("Sun, 25 May 2014 14:09:29 GMT");
			assertEquals(2014, d.getYear());
		}
		{
			DateTimeParser[] parsers = {
					DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss Z").getParser() //Mon, 26 May 2014 00:08:43 +0700
			};

			String s = DateTime.now().toString(DateTimeFormat.forPattern("EEE, dd MMM YYYY HH:mm:ss zzz"));
			System.out.println(s);

			DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();

			DateTime d = dateTimeFormatter.parseDateTime("Mon, 26 May 2014 00:08:43 +0700");
			assertEquals(2014, d.getYear());
		}
		{
			DateTime d = DateTimeFormat.forPattern("YYYY-MM-dd'T'HH:mm:ss'Z'").parseDateTime("2014-05-25T05:39:45Z");
			assertEquals(2014, d.getYear());
		}
		/*{
			DateTimeParser parser = ISODateTimeFormat.dateTimeParser().getParser();
			DateTimeParserBucket dateTimeParserBucket = null;

			parser.estimateParsedLength();
			parser.parseInto(dateTimeParserBucket, "2014-05-25T05:39:45Z", 0);

			DateTime d = ISODateTimeFormat.dateTimeParser().parseDateTime("2014-05-25T05:39:45Z");
			assertEquals(2014, d.getYear());
		}*/


	}

	public void testParseDateTimeFinal() {
		{
			DateTime d = DateUtils.parseDateTime("Mon, 26 May 2014 00:08:43 +0700");
			System.out.print(d.getZone().getID());
			assertEquals(2014, d.getYear());
		}
		{
			DateTime d = DateUtils.parseDateTime("2014-05-25T05:39:45Z");
			System.out.print(d.getZone().getID());
			assertEquals(2014, d.getYear());
		}
		{
			DateTime d = DateUtils.parseDateTime("Sun, 25 May 2014 14:09:29 GMT");
			System.out.print(d.getZone().getID());
			assertEquals(2014, d.getYear());
		}
		{
			DateTime d = DateUtils.parseDateTime("Sun, 25 May 2014 14:09:29 EDT");
			System.out.print(d.getZone().getID());
			assertEquals(2014, d.getYear());
		}
	}

}
