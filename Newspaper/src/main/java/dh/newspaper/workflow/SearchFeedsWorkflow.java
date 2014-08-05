package dh.newspaper.workflow;

import android.content.Context;
import android.webkit.URLUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SearchFeedsEvent;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.common.PerfWatcher;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.OncePrifoTask;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Compute feed sources from an input query
 * - Search query (input query is not a URL)
 * - Web Page which contains link to many feed sources (input query is an URL to web page)
 * - Direct page source (input query is URL of the feed sources)
 *
 * Hold state of the Subscription activity:
 * - Search result {@link #getSearchResultEvent()}
 * - Search error {@link #getSearchResultEvent()}
 *
 * Post events:
 * - {@link dh.newspaper.Constants#SUBJECT_SEARCH_FEEDS_START_LOADING}
 * - {@link dh.newspaper.Constants#SUBJECT_SEARCH_FEEDS_REFRESH}
 * - {@link dh.newspaper.Constants#SUBJECT_SEARCH_FEEDS_DONE_LOADING}
 *
 * Created by hiep on 23/07/2014.
 */
public class SearchFeedsWorkflow extends OncePrifoTask {
	//private static final String TAG = SearchFeedsTask.class.getName();
	private static final Logger Log = LoggerFactory.getLogger(SearchFeedsWorkflow.class);

	private volatile SearchFeedsEvent searchResultEvent;

	@Inject ObjectMapper objectMapper;
	@Inject ContentParser contentParser;
	@Inject RefData refData;
	private final String query;
	private PerfWatcher pf;

	public SearchFeedsWorkflow(Context context, String query) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		this.query = query;
		this.pf = new PerfWatcher(Log, query);
	}

	@Override
	public String getMissionId() {
		return query;
	}

	@Override
	public void perform() {
		try {
			searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_START_LOADING, query);
			EventBus.getDefault().post(searchResultEvent);

			if (URLUtil.isValidUrl(query)) {
				processUrlQuery();
			}
			else {
				processNormalQuery();
			}
		} catch (Exception ex) {
			pf.error("Search error", ex);
			searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_DONE_LOADING, query, ex);
			EventBus.getDefault().post(searchResultEvent);
		} finally {
			pf.dg("Search done");
		}
	}

	/**
	 * The input query is a valid URL:
	 * - It can be the feed source (directly)
	 * - I can be a web page which contains link to many other feed sources
	 * @throws IOException
	 */
	private void processUrlQuery() throws IOException {
		InputStream input = NetworkUtils.getStreamFromUrl(query, NetworkUtils.DESKTOP_USER_AGENT, this);
		if (input==null || isCancelled()) {return;}

		boolean queryIsDirectFeedsSource = false;
		Feeds feeds = null;
		try {
			Document doc = Jsoup.parse(input, Constants.DEFAULT_ENCODING, query, Parser.xmlParser());
			feeds = contentParser.parseFeeds(doc, this);
			if (feeds.size()>1) {
				queryIsDirectFeedsSource = true;
			}
		} catch (Exception ex) {
			pf.i("Query is not a valid feed source", ex);
		}

		if (queryIsDirectFeedsSource) {
			sendDirectFinalResult(feeds);
		}
		else {
			input.reset();
			processPageHtml(input);
		}
	}

	/**
	 *  The input query is a web page which contains links to feeds sources
	 */
	private void processPageHtml(InputStream input) throws IOException {
		if (isCancelled()) { return; }

		Document doc = Jsoup.parse(input, Constants.DEFAULT_ENCODING, query);

		if (isCancelled()) { return; }
		Elements links = doc.select("a[href]");
		if (links==null) {
			pf.d("No links found in web page");
			return;
		}

		pf.d("Found "+links.size()+" links in web page");
		SearchFeedsResult searchResult = new SearchFeedsResult();
		LinkedList<String> sources = new LinkedList<String>();

		//add all the links sources in the web page to the list so that the most
		// interesting address will be on the top of this list, remove all duplication
		int interestLink = 0;
		for (Element link : links) {
			if (isCancelled()) { return; }
			String src = link.attr("abs:href");

			if (sources.contains(src)) {
				continue; //ignore duplication link
			}

			if (mightBeFeedsSourcesAddress(src)) {
				sources.addFirst(src);
				interestLink++;
			}
			else {
				sources.addLast(src);
			}
		}

		pf.d("The web page might contain "+interestLink+" feeds sources addresses / "+sources.size()+" links");

		//visit all links extracted from the web page, start from most interesting one, which highly
		//probably a feeds sources
		for (String src : sources) {
			if (isCancelled()) { return; }
			if (!Strings.isNullOrEmpty(src) && URLUtil.isValidUrl(src)) {
				processLink(src, searchResult);
			}
		}

		sendFinalResult(searchResult);
	}

	private final static String[] FeedsSourcesAddressKeywords = new String[] {"feed", "atom", "rss"};

	/**
	 * The feeds sources address often contains some keyword (feed, atom, rss)
	 */
	private boolean mightBeFeedsSourcesAddress(String src) {
		for (String keyword : FeedsSourcesAddressKeywords) {
			if (src.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Detect if the link src is a feeds source, if yes, send a partial result to the GUI
	 */
	private void processLink(String src, SearchFeedsResult searchResult) {
		if (isCancelled()) { return; }
		try {
			String input = NetworkUtils.quickDownloadXml(src, NetworkUtils.DESKTOP_USER_AGENT, this);
			if (Strings.isNullOrEmpty(input)) {
				pf.d("Invalid feed source " + src+" not in xml format");
				return;
			}

			if (isCancelled()) { return; }
			Document doc = Jsoup.parse(input, query, Parser.xmlParser());

			if (isCancelled()) { return; }
			Feeds feeds = contentParser.parseFeeds(doc, this);

			if (feeds.size()>1) {
				insertResult(src, feeds, searchResult);
				pf.d("Detect valid feed source "+src);
			}

			if (isCancelled()) { return; }
			sendPartialResult(searchResult);
		} catch (Exception ex) {
			pf.d("Invalid feed source " + src, ex);
		}
	}

	/**
	 * Send Feeds as SearchFeedsResult
	 */
	private void sendDirectFinalResult(Feeds feeds) throws MalformedURLException {
		if (isCancelled()) { return; }
		pf.d("Query is a valid feed source, sendDirectResult "+feeds);

		//build result from source feeds
		SearchFeedsResult searchResult = new SearchFeedsResult();
		insertResult(query, feeds, searchResult);

		//send result
		sendFinalResult(searchResult);
	}

	/**
	 * add feeds to a search result
	 */
	private void insertResult(String feedsLink, Feeds feeds, SearchFeedsResult searchResult) throws MalformedURLException {
		SearchFeedsResult.ResponseData.Entry entry = new SearchFeedsResult.ResponseData.Entry();
		entry.setContentSnippet(feeds.getDescription());
		entry.setTitle(StrUtils.hostName(feeds.getUrl()));
		entry.setLink(feeds.getUrl());
		entry.setUrl(feedsLink);
		entry.setValidity(SearchFeedsResult.FeedsSourceValidity.OK);
		entry.setFeeds(feeds);

		if (searchResult.getResponseData() == null) {
			searchResult.setResponseData(new SearchFeedsResult.ResponseData());
		}

		if (searchResult.getResponseData().getEntries() == null) {
			searchResult.getResponseData().setEntries(new ArrayList<SearchFeedsResult.ResponseData.Entry>());
		}

		//check if the result is already subscribed
		entry.setSubscription(refData.findSubscription(feedsLink));

		searchResult.getResponseData().getEntries().add(entry);
	}

	/**
	 * The query is not a valid URL: we use Google Feed API to search feed sources
	 * @throws IOException
	 */
	private boolean processNormalQuery() throws IOException {
		final String queryUrl = "https://ajax.googleapis.com/ajax/services/feed/find?v=1.0&q="+ URLEncoder.encode(query, "UTF-8");
		pf.d("processNormalQuery "+queryUrl);
		if (isCancelled()) { return true; }

		byte[] rawData = NetworkUtils.downloadContent(queryUrl, NetworkUtils.DESKTOP_USER_AGENT, this);

		if (isCancelled()) { return true; }
		SearchFeedsResult searchResult = objectMapper.readValue(rawData, SearchFeedsResult.class);


		if (isCancelled()) { return true; }

		//check if the result is already subscribed then set the subscription properties
		refData.matchExistSubscriptions(searchResult);

		if (isCancelled()) { return true; }
		sendFinalResult(searchResult);
		return false;
	}

	private void sendFinalResult(SearchFeedsResult searchResult) {
		searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_DONE_LOADING, query, searchResult);
		EventBus.getDefault().post(searchResultEvent);
	}
	private void sendPartialResult(SearchFeedsResult searchResult) {
		searchResultEvent = new SearchFeedsEvent(this, Constants.SUBJECT_SEARCH_FEEDS_REFRESH, query, searchResult);
		EventBus.getDefault().post(searchResultEvent);
	}

	public String getQuery() {
		return query;
	}

	public SearchFeedsEvent getSearchResultEvent() {
		return searchResultEvent;
	}
}
