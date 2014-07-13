package dh.tool.justext;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by hiep on 28/06/2014.
 */
public class ConfigurationTest {
	@Test
	public void testSplitter() {
		String configStr = "lengthHigh = 20; maxLinkDensity=0.5;\n\r removeTitle = true; \t language=vn";
		ArrayList<String> props = Lists.newArrayList(Splitter.on(Pattern.compile(";\\s")).trimResults().omitEmptyStrings().split(configStr));
		Assert.assertEquals(4, props.size());
	}
	@Test
	public void testLoadConfig() throws ParseException {
		String configStr = "lengthHigh = 20; maxLinkDensity=0.5;\n\r removeTitle = true; \t language=vn";
		Configuration config = new Configuration.Builder(configStr).build();
		Assert.assertEquals(20, config.lengthHigh());
		Assert.assertEquals(0.5, config.maxLinkDensity());
		Assert.assertTrue(config.removeTitle());
		Assert.assertEquals("Vietnamese", config.language());
	}
}
