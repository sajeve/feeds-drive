package dh.newspaper.test;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import dh.newspaper.MainActivity;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.model.FeedItem;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.tools.NetworkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DownloadAndSaveFileTest extends ActivityInstrumentationTestCase2<MainActivity> {

	public DownloadAndSaveFileTest() {
		super(MainActivity.class);
	}

	static final String TAG = DownloadAndSaveFileTest.class.getName();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		System.setProperty("http.agent", "");
	}

	public void testDownloadHtmlToFile() throws IOException {
		Context ctx = this.getActivity();
		Log.i(TAG, "Begin test");
		String address = "http://kinhdoanh.vnexpress.net/tin-tuc/ebank/sap-nhap-ngan-hang-yeu-co-khien-kho-khan-bi-cong-don-2985213.html";
		{
			InputStream input = NetworkUtils.getStreamFromUrl(address, NetworkUtils.MOBILE_USER_AGENT);
			TestUtils.writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.UrlConnection.html", input, false);
			input.close();
		}
		{
			InputStream input = NetworkUtils.getStreamFromUrl(address, NetworkUtils.MOBILE_USER_AGENT);
			TestUtils.writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.HttpGet.html", input, false);
			input.close();
		}
		Log.i(TAG, "Finish test");
	}

    public void testEmptyCase() throws IOException {
		InputStream input = NetworkUtils.getStreamFromUrl("http://kinhdoanh.vnexpress.net/tin-tuc/ebank/sap-nhap-ngan-hang-yeu-co-khien-kho-khan-bi-cong-don-2985213.html", NetworkUtils.MOBILE_USER_AGENT);
		Document doc=Jsoup.parse(input, "utf-8", "ssd");

		assertNotNull(doc);
		doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		System.out.println(doc.html());
    }

	public void testGetRssItem() throws FeedParserException, IOException {
		ContentParser contentParser = new ContentParser();
		List<FeedItem> items = contentParser.parseFeeds("http://vnexpress.net/rss/thoi-su.rss", "UTF-8");
		assertTrue(items.size() > 0);
	}
}
