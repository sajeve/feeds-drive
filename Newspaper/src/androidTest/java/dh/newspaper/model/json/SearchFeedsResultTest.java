package dh.newspaper.model.json;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.squareup.okhttp.OkHttpClient;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.test.TestUtils;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.view.SubscriptionActivity;
import dh.tool.thread.ICancellation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class SearchFeedsResultTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private final static String TAG = SearchFeedsResultTest.class.getName();

	//BackgroundTasksManager backgroundTasksManager;

	public SearchFeedsResultTest() {
		super(MainActivity.class);
	}

	public void testSearchFeedsSources() throws IOException {
		//backgroundTasksManager = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(BackgroundTasksManager.class);

		final String query = "Công nghệ";
		final String queryUrl = "https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q="+URLEncoder.encode(query, "UTF-8");

		byte[] rawData = NetworkUtils.downloadContent(queryUrl, NetworkUtils.DESKTOP_USER_AGENT, null);

		ObjectMapper mapper = new ObjectMapper();
		SearchFeedsResult o = mapper.readValue(rawData, SearchFeedsResult.class);
		assertTrue(o.getResponseData().getEntries().size() > 0);
	}



}