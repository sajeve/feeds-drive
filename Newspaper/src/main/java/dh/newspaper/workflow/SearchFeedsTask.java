package dh.newspaper.workflow;

import android.content.Context;
import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.event.SearchFeedsEvent;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.thread.prifo.PrifoTask;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hold state of the Subscription activity:
 * - Search result {@link #getSearchResultEvent()}
 * - Search error {@link #getSearchResultEvent()}
 * - Progression {@link #isDone()}
 * Created by hiep on 23/07/2014.
 */
public class SearchFeedsTask extends PrifoTask {
	private static final String TAG = SearchFeedsTask.class.getName();

	private final ReentrantLock lock = new ReentrantLock();

	private volatile boolean done = false;
	private volatile SearchFeedsEvent searchResultEvent;

	@Inject ObjectMapper objectMapper;
	private final String query;

	public SearchFeedsTask(Context context, String query) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		this.query = query;
	}

	@Override
	public String getMissionId() {
		return query;
	}

	@Override
	public void run() {
		if (isCancelled()) { return; }

		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			done = false;
			final String queryUrl = "https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q="+ URLEncoder.encode(query, "UTF-8");

			if (isCancelled()) { return; }
			searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_START_LOADING, query);
			EventBus.getDefault().post(searchResultEvent);

			byte[] rawData = NetworkUtils.downloadContent(queryUrl, NetworkUtils.DESKTOP_USER_AGENT, this);

			if (isCancelled()) { return; }
			SearchFeedsResult searchResult = objectMapper.readValue(rawData, SearchFeedsResult.class);

			done = true;
			if (isCancelled()) { return; }
			searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_REFRESH, query, searchResult);
			EventBus.getDefault().post(searchResultEvent);
		} catch (Exception ex) {
			Log.w(TAG, ex);
			searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_REFRESH, query, ex);
			EventBus.getDefault().post(searchResultEvent);
		} finally {
			lock.unlock();
		}
	}

	public String getQuery() {
		return query;
	}

	public SearchFeedsEvent getSearchResultEvent() {
		return searchResultEvent;
	}

	public boolean isDone() {
		return done;
	}
}
