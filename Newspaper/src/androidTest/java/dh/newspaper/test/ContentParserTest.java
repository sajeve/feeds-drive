package dh.newspaper.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.MainActivity;
import dh.newspaper.tools.NetworkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import dh.newspaper.parser.ContentParser;

public class ContentParserTest extends ActivityInstrumentationTestCase2<MainActivity> {

	ContentParser contentParser;

	public ContentParserTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		contentParser = new ContentParser();
	}

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
}
