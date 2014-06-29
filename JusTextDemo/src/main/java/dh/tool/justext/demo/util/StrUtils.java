//package dh.tool.justext.demo.util;
//
//import com.google.common.base.Strings;
//
///**
// * Created by hiep on 14/05/2014.
// */
//public class StrUtils {
//	private static final String TAG = StrUtils.class.getName();
//	private final static int GLIMPSE_SIZE = 60;
//	private final static String NON_THIN = "[^iIl1\\.,']";
//
//	private static int textWidth(String str) {
//		return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
//	}
//
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
//	/**
//	 * Remove double white space, and convert to upper case
//	 */
//	public static String normalizeUpper(String s) {
//		return s.trim().replaceAll("\\s+", " ").toUpperCase();
//	}
//
//	/**
//	 * Same as {@link String#equalsIgnoreCase(String)}
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
//	 * Same as {@link String#equals(Object)}
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
//		int lenStr = Strings.isNullOrEmpty(str) ? 0 : str.length();
//		int lenRef =  Strings.isNullOrEmpty(ref) ? 0 : ref.length();
//		int shortestLenAllowed = lenRef*percent/100;
//		return lenStr < shortestLenAllowed;
//	}
//}
