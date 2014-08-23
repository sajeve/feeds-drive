package dh.tool.common;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by hiep on 29/07/2014.
 */
public class StrUtilsTest {
	@Test
	public void testRemoveTrailingSlash() {
		String str = "http://nytimes.com/us/home";
		Assert.assertEquals(str, StrUtils.removeTrailingSlash(str));
		Assert.assertEquals(str, StrUtils.removeTrailingSlash(str+"/"));
		Assert.assertEquals(str, StrUtils.removeTrailingSlash(str+"//"));
	}
}
