package dh.tool.common;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Created by hiep on 14/05/2014.
 */
public class StrUtils {
	private static final String TAG = StrUtils.class.getName();
	private final static int GLIMPSE_SIZE = 60;
	private final static String NON_THIN = "[^iIl1\\.,']";
	private final static char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	private static int textWidth(String str) {
		return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
	}

	private static String cut(String text, int max, String trail, boolean includeSize) {
		if (text == null) {
			return "null";
		}

		String suffix = trail;
		if (includeSize) {
			suffix = suffix + " (length="+text.length()+")";
		}
		int suffixLength = suffix.length();

		if (textWidth(text) <= max)
			return text;

		// Start by chopping off at the word before max
		// This is an over-approximation due to thin-characters...
		int end = text.lastIndexOf(' ', max-suffixLength);

		// Just one long word. Chop it off.
		if (end == -1)
			return text.substring(0, max-suffixLength) + suffix;

		// Step forward as long as textWidth allows.
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = text.indexOf(' ', end + 1);

			// No more spaces.
			if (newEnd == -1)
				newEnd = text.length();

		} while (textWidth(text.substring(0, newEnd) + suffix) < max);

		return text.substring(0, end) + suffix;
	}

	/**
	 * Use to log a long string, give a quick glimpse at the first 60 characters and replace return line by space
	 */
	public static String glimpse(String text) {
		if (text == null) {
			return "null";
		}
		if (text.length()<GLIMPSE_SIZE) {
			return text;
		}
		return text.substring(0, GLIMPSE_SIZE).replace('\n', ' ').replace('\r', ' ')+".. (length="+text.length()+")";
	}

	public static String ellipsize(String text, int max) {
		return cut(text, max, "...", false);
	}

	public static String cut(String text, int max) {
		return cut(text, max, "", false);
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

	/**
	 * Remove double white space, and convert to upper case
	 */
	public static String normalizeUpper(String s) {
		return s.trim().replaceAll("\\s+", " ").toUpperCase();
	}

	/**
	 * Same as {@link String#equalsIgnoreCase(String)}
	 * prevent NullPointerException, return true if both a and b is Null or Empty
	 */
	public static boolean equalsIgnoreCases(String a, String b) {
		if (Strings.isNullOrEmpty(a)) {
			return Strings.isNullOrEmpty(b);
		}
		return a.equalsIgnoreCase(b);
	}

	/**
	 * Same as {@link String#equals(Object)}
	 * prevent NullPointerException, return true if both a and b is Null or Empty
	 */
	public static boolean equalsString(String a, String b) {
		if (Strings.isNullOrEmpty(a)) {
			return Strings.isNullOrEmpty(b);
		}
		return a.equals(b);
	}

	/**
	 * return true if str is shorter than percent of ref
	 */
	public static boolean tooShort(String str, String ref, int percent) {
		if (percent < 0) {
			return false;
		}
		int lenStr = Strings.isNullOrEmpty(str) ? 0 : str.length();
		int lenRef =  Strings.isNullOrEmpty(ref) ? 0 : ref.length();
		int shortestLenAllowed = lenRef*percent/100;
		return lenStr < shortestLenAllowed;
	}

	public static String domainName(String url) throws MalformedURLException {
		String hostName = (new URL(url)).getHost();
		//remove prefix "www."
		if (hostName.startsWith("www.")) {
			hostName = hostName.substring(4, hostName.length());
		}
		return hostName;
	}

	public static String toString(final InputStream input, Charset encoding) throws IOException {
		InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
			@Override
			public InputStream getInput() throws IOException {
				return input;
			}
		};
		InputSupplier<InputStreamReader> readerSupplier =
				CharStreams.newReaderSupplier(inputSupplier, encoding);

		return CharStreams.toString(readerSupplier);
	}

	public static Charset getCharset(String charset) {
		if (!Strings.isNullOrEmpty(charset)) {
			if (charset.equalsIgnoreCase("UTF_8") || charset.equalsIgnoreCase("UTF8") || charset.equalsIgnoreCase("UTF-8")) {
				return Charsets.UTF_8;
			}
			if (charset.equalsIgnoreCase("ISO_8859_1") || charset.equalsIgnoreCase("ISO-8859-1") || charset.equalsIgnoreCase("ISO-8859")) {
				return Charsets.ISO_8859_1;
			}
			if (charset.equalsIgnoreCase("UTF_16") || charset.equalsIgnoreCase("UTF16") || charset.equalsIgnoreCase("UTF-16")) {
				return Charsets.UTF_16;
			}
			if (charset.equalsIgnoreCase("US_ASCII") || charset.equalsIgnoreCase("ASCII")) {
				return Charsets.US_ASCII;
			}
			if (charset.equalsIgnoreCase("UTF_16BE") || charset.equalsIgnoreCase("UTF-16-BE") || charset.equalsIgnoreCase("UTF16BE")  || charset.equalsIgnoreCase("UTF_16_BE")) {
				return Charsets.UTF_16BE;
			}
			if (charset.equalsIgnoreCase("UTF_16LE") || charset.equalsIgnoreCase("UTF-16-LE") || charset.equalsIgnoreCase("UTF16LE") || charset.equalsIgnoreCase("UTF_16_LE")) {
				return Charsets.UTF_16LE;
			}
		}
		return Charset.defaultCharset();
	}

	public static String removeTrailingSlash(String str) {
		return str.replaceFirst("/*$", "");
	}
}
