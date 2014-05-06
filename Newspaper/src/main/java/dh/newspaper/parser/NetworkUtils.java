package dh.newspaper.parser;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hiep on 7/05/2014.
 */
public class NetworkUtils {
	private static final String TAG = NetworkUtils.class.getName();
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";

	/**
	 * Get network stream (mobile user-agent) use HttpURLConnection
	 * @param address
	 * @return
	 * @throws IOException
	 */
	public static InputStream getStreamFromUrl2(String address) throws IOException {
		Log.d(TAG, "Create HttpURLConnection to " + address);

		URL url = new URL(address);
		HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
		httpConnection_.addRequestProperty("User-Agent", MOBILE_USER_AGENT);
		int responseCode = httpConnection_.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK)
			return httpConnection_.getInputStream();
		else
			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
	}

	/**
	 * Get network stream (mobile user-agent) use HttpGet
	 * @param address
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException {
		Log.d(TAG, "Create HttpClient to "+address);

		HttpGet httpGet_ = new HttpGet(address);
		HttpClient httpclient = AndroidHttpClient.newInstance(MOBILE_USER_AGENT);

		// Execute HTTP Get Request
		HttpResponse response = httpclient.execute(httpGet_);
		return response.getEntity().getContent();
	}
}
