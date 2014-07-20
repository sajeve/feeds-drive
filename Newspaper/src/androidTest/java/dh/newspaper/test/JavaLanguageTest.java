package dh.newspaper.test;

import com.google.common.base.Strings;
import dh.newspaper.tools.DateUtils;
import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by hiep on 10/05/2014.
 */
public class JavaLanguageTest extends TestCase {

	String getMd5(String s) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		return DateUtils.bytesToHex(md5.digest(s.toUpperCase().replaceAll("\\s", "").getBytes("UTF-8")));
	}

	public void testCalculateChecksum() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		String s1 = getMd5("Lorem ipsum dolor sit amet, consectetuer adipiscing ELIT  ");
		String s2 = getMd5("Lorem ipsum      dolor sit amet, consectetuer    ADipiscing elit");
		assertEquals(s1, s2);
	}


}
