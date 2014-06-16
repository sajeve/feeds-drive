package dh.newspaper.parser;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.test.TestUtils;
import dh.newspaper.tools.NetworkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;

public class ContentParserTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private final String TAG = ContentParserTest.class.getName();

	ContentParser contentParser;

	public ContentParserTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		contentParser = new ContentParser();
	}

	public void runExtractContent(String address, String fileName, String xpath) throws IOException {
		//download
		InputStream in = NetworkUtils.getStreamFromUrl(address, NetworkUtils.MOBILE_USER_AGENT, null);

		//parse
		Document doc = Jsoup.parse(in, Constants.DEFAULT_ENCODING, address);

		//clean a little
		ContentParser.removeComments(doc);
		doc.select("script").remove();

		//write to file
		TestUtils.writeToFile(fileName, doc.html(), false);

		//extract content
		String content = contentParser.extractContent(doc, xpath, new StringBuilder()).html();

		Log.i(TAG, content);
		assertFalse(Strings.isNullOrEmpty(content));

		//write content to file
		TestUtils.writeToFile(fileName+" content.html", content, true);
	}

	public void testNytimes1() throws IOException {
		runExtractContent(
				"http://www.nytimes.com/2014/06/17/business/gm-recalls-3-million-more-cars.html",
				Constants.DEBUG_DATABASE_PATH + "/test/gm-recalls-3-million-more-cars.html",
				"p.story-body-text, figure {or} div.article-body"
		);
	}

	public void testJsoup() {
		Jsoup.parse("<img src='abc.htm' width='1' height='10'>", "localhost");

	}
}

/*

	public void testExtractVnexpressTest() throws IOException {
		String address = "http://vnexpress.net/tin-tuc/thoi-su/cau-rong-nhan-giai-ky-thuat-xuat-sac-quoc-te-2985651.html";
		String vnexpressContentSelector = "div.short_intro, div.relative_new, div.fck_detail";

		String content = contentParser.extractContent(address, vnexpressContentSelector).html();

		System.out.println(content);
		assertTrue(content.length() > 50);
		TestUtils.writeToFile(getActivity().getExternalFilesDir(null) + "/vnexpress.sample.html", content, true);
	}

	public void testExtractNYTimes() throws IOException {
		String address = "http://www.nytimes.com/2014/05/11/realestate/wanted-a-brooklyn-apartment-with-more-space.html?hpw&rref=realestate&_r=0";
		String contentSelector = "div.article-body";

		String content = contentParser.extractContent(address, contentSelector).html();

		System.out.println(content);
		assertTrue(content.length() > 100);
		TestUtils.writeToFile(getActivity().getExternalFilesDir(null) + "/nytimes.sample.html", content, true);
	}

	public void testDownloadContent() throws IOException {
		String address = "http://www.nytimes.com/2014/05/11/realestate/wanted-a-brooklyn-apartment-with-more-space.html?hpw&rref=realestate&_r=0";

		Document doc = Jsoup.connect(address).userAgent(NetworkUtils.MOBILE_USER_AGENT).get();
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		String content = doc.body().html();

		TestUtils.writeToFile(getActivity().getExternalFilesDir(null) + "/nytimes.body.html", content, true);
	}

	public static void testBasicParseString() {
		String source = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>xướng tên</body></html>";
		Document doc = Jsoup.parse(source);
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		System.out.println(doc.toString());
	}

	public static void testBasicParseFile() throws IOException {
		String source = "vnexpress.source.html";
		Document doc = Jsoup.parse(new File(source), "UTF-8");
		assertNotNull(doc);
		System.out.println(doc.toString());
	}
 */