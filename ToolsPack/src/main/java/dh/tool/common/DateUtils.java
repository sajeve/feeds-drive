package dh.tool.common;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by hiep on 30/08/2014.
 */
public class DateUtils {
	public static Date createDate(int year, int month, int dayOfMonth) {
		Calendar calendar = new GregorianCalendar(year, month, dayOfMonth);
		return calendar.getTime();
	}
}
