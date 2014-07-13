package dh.tool.justext.demo.util;

import com.google.common.base.Stopwatch;
import dh.tool.justext.demo.MainApp;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 13/07/2014.
 */
public class Network {
	private static final Logger Log = LogManager.getLogger(MainApp.class);

	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36";

	public static Document downloadPage(String address, boolean asMobileAgent) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		byte[] rawContent = downloadContentHttpGet(address, asMobileAgent ? MOBILE_USER_AGENT : DESKTOP_USER_AGENT);
		Document doc = Jsoup.parse(new ByteArrayInputStream(rawContent), "utf-8", address);
		sw.stop();
		Log.info(String.format("Download and parse: %d ms - '%s'", sw.elapsed(TimeUnit.MILLISECONDS), address));
		return doc;
	}

	/**
	 * Download content using HttpGet
	 */
	public static byte[] downloadContentHttpGet(String address, String userAgent) throws IOException {
		HttpClient httpClient = HttpClientBuilder.create()
				.setUserAgent(userAgent)
				.build();

		HttpGet request = new HttpGet(address);
		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();
		byte[] content = EntityUtils.toByteArray(entity);
		return content;
	}

	private Document downloadPage2(String address, boolean asMobileAgent) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		Connection con = HttpConnection.connect(new URL(address));
		con.userAgent(asMobileAgent ? MOBILE_USER_AGENT : DESKTOP_USER_AGENT).timeout(60*1000);
		Document doc = con.get();
		sw.stop();
		Log.info(String.format("Download and parse: %d ms - '%s'", sw.elapsed(TimeUnit.MILLISECONDS), address));
		return doc;
	}
}
