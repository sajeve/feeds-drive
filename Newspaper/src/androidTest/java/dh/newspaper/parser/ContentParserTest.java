package dh.newspaper.parser;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.squareup.okhttp.OkHttpClient;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.test.TestUtils;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.thread.ICancellation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class ContentParserTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private final static String TAG = ContentParserTest.class.getName();

	ContentParser contentParser;

	public ContentParserTest() {
		super(MainActivity.class);
	}

	@Override
	protected void setUp() throws Exception {
		contentParser = new ContentParser();
	}









	public void testDownloadWithRedirect() throws IOException {
		String address = "http://www.nytimes.com/2014/07/07/world/middleeast/dozens-killed-in-yemen-as-sectarian-muslim-fighting-heats-up.html";
		//byte[] data = downloadContentHttpConnection(address, NetworkUtils.DESKTOP_USER_AGENT, null);
		byte[] data = NetworkUtils.downloadContentHttpGet(address, NetworkUtils.DESKTOP_USER_AGENT,  null);

		InputStream dataStream = new ByteArrayInputStream(data);
		TestUtils.writeToFile(Constants.DEBUG_DATABASE_PATH + "/nytime_dozens4_noredirect.html", dataStream, false);
	}

	private static final OkHttpClient okHttpClient = new OkHttpClient();
	/**
	 * Download content using HttpConnection
	 */
	public static byte[] downloadContentHttpConnection(String address, String userAgent, ICancellation cancelListener) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();

		HttpURLConnection httpConnection = okHttpClient.open(new URL(address));
		httpConnection.addRequestProperty("User-Agent", userAgent);

		int responseCode = httpConnection.getResponseCode();
		InputStream input = httpConnection.getInputStream();

		ByteArrayOutputStream baos = null;
		try {
			//download all the page to other InputStream
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = input.read(buffer)) > -1) {
				if (cancelListener!=null && cancelListener.isCancelled()) {
					Log.v(TAG, "Download canceled ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
					return null;
				}
				baos.write(buffer, 0, len);
			}
			baos.flush();

			if (200<=responseCode && responseCode<400) {
				byte[] content = baos.toByteArray();
				baos.close();
				Log.v(TAG, "Download end "+content.length+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
				return content;
			} else {
				Log.w(TAG, "status code is "+responseCode);
				return null;
				//throw new IllegalStateException("Failed to connect to " + address + ": " + httpConnection.getResponseMessage() + " (" + responseCode + ")");
			}
		}
		finally {
			if (input != null) {
				input.close();
			}
			if (baos!=null) {
				baos.close();
			}
		}
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

	public void testExtractContent() throws IOException {
		String address = "http://vnexpress.net/video/bong-da/nhung-pha-nga-vo-lo-lieu-cua-robben-3011940.html";
		InputStream inputStream = NetworkUtils.getStreamFromUrl(address, NetworkUtils.DESKTOP_USER_AGENT, null);

//		Logger log = LoggerFactory.getLogger(ContentParserTest.class);
//		PerfWatcher pw = new PerfWatcher(log, address);

		StringBuilder notice = new StringBuilder();
		Document doc = contentParser.extractContent(inputStream, Constants.DEFAULT_ENCODING, address, notice, null);

//		pw.t("Finished verbose");
//		pw.i("Finised extracting");

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