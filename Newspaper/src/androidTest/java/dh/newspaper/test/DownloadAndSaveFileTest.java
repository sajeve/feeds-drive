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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.Jsoup;
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
			InputStream input = getStreamFromUrl2(address);
			writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.UrlConnection.html", input, false);
			input.close();
		}
		{
			InputStream input = getStreamFromUrl(address);
			writeToFile(ctx.getExternalFilesDir(null)+"/vnexpress.HttpGet.html", input, false);
			input.close();
		}
		Log.i(TAG, "Finish test");
	}

    public void testEmptyCase() {

    }


	protected InputStream getStreamFromUrl2(String address) throws IOException {
		Log.d(TAG, "Create HttpURLConnection to "+address);

        URL url = new URL(address);
        HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
        httpConnection_.addRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");
        int responseCode = httpConnection_.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK)
            return httpConnection_.getInputStream();
        else
            throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
    }

    protected InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException
    {
    	Log.d(TAG, "Create HttpClient to "+address);
    	HttpGet httpGet_ = new HttpGet(address);
        HttpClient httpclient = AndroidHttpClient.newInstance("Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30");

        // Execute HTTP Get Request
        HttpResponse response = httpclient.execute(httpGet_);
        return response.getEntity().getContent();
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
