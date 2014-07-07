package dh.newspaper.tools;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.squareup.okhttp.OkHttpClient;
import dh.newspaper.Constants;
import dh.tool.common.ICancellation;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 7/05/2014.
 */
public class NetworkUtils {
	private static final String TAG = NetworkUtils.class.getName();
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36";
	private static final int SOCKET_OPERATION_TIMEOUT = 60 * 1000;

	private static final OkHttpClient okHttpClient = new OkHttpClient();

	/**
	 * Get network stream (mobile user-agent) use HttpGet
	 * @param address
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static byte[] downloadContentAndroidHttpGet(String address, ICancellation cancelListener) throws IOException {
		Stopwatch sw = null;
		if (Constants.DEBUG) {
			sw = Stopwatch.createStarted();
		}

		AndroidHttpClient ahClient = AndroidHttpClient.newInstance(DESKTOP_USER_AGENT);
		ahClient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, true);
		ahClient.getParams().setParameter(ClientPNames.MAX_REDIRECTS, 40);
		ahClient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true); //Too many redirects: 21

		HttpGet request = new HttpGet(address);
		byte[] rawData = null;
		try {
			if (cancelListener!=null && cancelListener.isCancelled()) {
				Log.v(TAG, "Downloaded canceled AndroidHttpClient ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
			}

			HttpResponse response = ahClient.execute(request);
			HttpEntity entity = response.getEntity();

			return EntityUtils.toByteArray(entity);
		} finally {
			ahClient.close();

			if (Constants.DEBUG) {
				sw.stop();
				int size = rawData == null ? 0 : rawData.length;
				Log.v(TAG, "Downloaded end HttpGet "+size+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
			}
		}
	}
	/**
	 * Download content using HttpGet
	 */
	public static byte[] downloadContentHttpGet(String address, String userAgent, ICancellation cancelListener) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();

		HttpClient httpClient = new DefaultHttpClient();
		HttpProtocolParams.setUserAgent(httpClient.getParams(), userAgent); // or httpClient.getParams().setParameter("http.useragent", userAgent);
		httpClient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true); //Too many redirects: 21
		HttpConnectionParams.setStaleCheckingEnabled(httpClient.getParams(), false);
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), SOCKET_OPERATION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpClient.getParams(), SOCKET_OPERATION_TIMEOUT);
		HttpConnectionParams.setSocketBufferSize(httpClient.getParams(), 8192);

		HttpGet request = new HttpGet(address);

		if (cancelListener!=null && cancelListener.isCancelled()) {
			Log.v(TAG, "Download HG cancelled  ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
		}

		HttpResponse response = httpClient.execute(request);
		HttpEntity entity = response.getEntity();

		byte[] content = EntityUtils.toByteArray(entity);

		Log.v(TAG, "Download HG end "+content.length+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
		return content;
	}

	/**
	 * Download content using HttpConnection
	 */
	public static byte[] downloadContentHttpConnection(String address, String userAgent, ICancellation cancelListener) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();

		HttpURLConnection httpConnection = okHttpClient.open(new URL(address));
		httpConnection.addRequestProperty("User-Agent", userAgent);
		int responseCode = httpConnection.getResponseCode();

		if (200<=responseCode && responseCode<300) {
			InputStream input = httpConnection.getInputStream();
			ByteArrayOutputStream baos = null;
			try {
				//download all the page to other InputStream
				baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len;
				while ((len = input.read(buffer)) > -1) {
					if (cancelListener!=null && cancelListener.isCancelled()) {
						Log.v(TAG, "Download HC canceled ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
						return null;
					}
					baos.write(buffer, 0, len);
				}
				baos.flush();

				byte[] content = baos.toByteArray();
				baos.close();
				Log.v(TAG, "Download HC end "+content.length+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
				return content;
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
		else {
			throw new IllegalStateException("Failed to connect to " + address + ": " + httpConnection.getResponseMessage() + " (" + responseCode + ")");
		}
	}


//	/**
//	 * Get network stream (mobile user-agent) use HttpURLConnection
//	 * @param address
//	 * @return
//	 * @throws IOException
//	 */
//	static InputStream getStreamFromUrl2(String address, String userAgent) throws IOException {
//		Log.d(TAG, "Create HttpURLConnection to " + address);
//
//		URL url = new URL(address);
//		HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
//		httpConnection_.addRequestProperty("User-Agent", userAgent);
//		int responseCode = httpConnection_.getResponseCode();
//		if (responseCode == HttpURLConnection.HTTP_OK)
//			return httpConnection_.getInputStream();
//		else
//			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
//	}

	/**
	 * Get content stream use HttpURLConnection, if failed, retry with HttpGet
	 */
	public static InputStream getStreamFromUrl(String address, String userAgent, ICancellation cancelListener) throws IOException {
		byte[] content = downloadContent(address, userAgent, cancelListener);
		return content==null ? null : new ByteArrayInputStream(content);
	}

	/**
	 * Download content with HttpConnection, if failed, retry with HttpGet
	 */
	public static byte[] downloadContent(String address, String userAgent, ICancellation cancelListener) throws IOException {
		byte[] content;
		try {
			content = downloadContentHttpGet(address, userAgent, cancelListener);
		}
		catch (Exception ex) {
			Log.w(TAG, ex.toString() + " on " + address + ". Retry with HttpConnection");
			content = downloadContentHttpConnection(address, userAgent, cancelListener);
		}
		return content;
	}
}

//	/**
//	 * Get network stream (mobile user-agent) use HttpURLConnection
//	 * @param address
//	 * @return
//	 * @throws IOException
//	 */
//	static InputStream getStreamFromUrl2(String address) throws IOException {
//		Log.d(TAG, "Create HttpURLConnection to " + address);
//
//		URL url = new URL(address);
//		HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
//		httpConnection_.addRequestProperty("User-Agent", MOBILE_USER_AGENT);
//		int responseCode = httpConnection_.getResponseCode();
//		if (responseCode == HttpURLConnection.HTTP_OK)
//			return httpConnection_.getInputStream();
//		else
//			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
//	}

//	/**
//	 * Get network stream (mobile user-agent) use HttpGet
//	 * @param address
//	 * @return
//	 * @throws IllegalStateException
//	 * @throws IOException
//	 */
//	public static InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException {
//		Log.d(TAG, "Create HttpClient to "+address);
//
//		HttpGet httpGet_ = new HttpGet(address);
//		HttpClient httpclient = AndroidHttpClient.newInstance(MOBILE_USER_AGENT);
//
//		// Execute HTTP Get Request
//		HttpResponse response = httpclient.execute(httpGet_);
//
//		//ByteArrayInputStream
//
//		return response.getEntity().getContent();
//
//	}