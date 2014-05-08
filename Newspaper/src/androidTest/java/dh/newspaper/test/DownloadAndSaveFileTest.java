package dh.newspaper.test;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import dh.newspaper.parser.NetworkUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import dh.newspaper.MainActivity;

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
			InputStream input = NetworkUtils.getStreamFromUrl(address);
			writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.UrlConnection.html", input, false);
			input.close();
		}
		{
			InputStream input = NetworkUtils.getStreamFromUrl(address);
			writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.HttpGet.html", input, false);
			input.close();
		}
		Log.i(TAG, "Finish test");
	}

    public void testEmptyCase() throws IOException {
		InputStream input = NetworkUtils.getStreamFromUrl("http://kinhdoanh.vnexpress.net/tin-tuc/ebank/sap-nhap-ngan-hang-yeu-co-khien-kho-khan-bi-cong-don-2985213.html");
		Document doc=Jsoup.parse(input, "utf-8", "ssd");

		assertNotNull(doc);
		doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		System.out.println(doc.html());
    }

    private static void writeToFile(String filename, String content, boolean wrapHtml) throws IOException {
    	Log.d(TAG, "Start write to file "+filename);

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));

		if (wrapHtml) {
			writer.write("<!DOCTYPE html>\n");
			writer.write("<html><head><meta charset=\"utf-8\"></head><body>");
		}
		writer.write(content);
		if (wrapHtml) {
			writer.write("</body></html>");
		}
		writer.close();
	}

    private static void writeToFile(String filename, InputStream content, boolean wrapHtml) throws IOException {
    	Log.d(TAG, "Start write to file "+filename);

    	BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
		if (wrapHtml) {
			writer.write("<!DOCTYPE html>\n");
			writer.write("<html><head><meta charset=\"utf-8\"></head><body>");
		}

		BufferedInputStream bis = new BufferedInputStream(content);
		InputStreamReader sr = new InputStreamReader(bis);

		char[] buffer = new char[100];
		while (sr.read(buffer) > 0) {
			writer.write(buffer);
		}

		if (wrapHtml) {
			writer.write("</body></html>");
		}
		writer.close();
	}
}
