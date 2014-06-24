package dh.tool.justext;

import com.squareup.okhttp.OkHttpClient;
import dh.tool.common.ICancellation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36";

	private static final OkHttpClient okHttpClient = new OkHttpClient();

	/**
	 * Download content using HttpConnection
	 */
	public static byte[] downloadContent(String address, String userAgent, ICancellation cancelListener) throws IOException {
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
					return null;
				}
				baos.write(buffer, 0, len);
			}
			baos.flush();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				byte[] content = baos.toByteArray();
				baos.close();
				return content;
			} else {
				throw new IllegalStateException("Failed to connect to " + address + ": " + httpConnection.getResponseMessage() + " (" + responseCode + ")");
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

	/**
	 * Get content stream use HttpURLConnection, if failed, retry with HttpGet
	 */
	public static InputStream getStreamFromUrl(String address, String userAgent, ICancellation cancelListener) throws IOException {
		byte[] content = downloadContent(address, userAgent, cancelListener);
		return content==null ? null : new ByteArrayInputStream(content);
	}
}
