package dh.newspaper.parser;

import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.Subscription;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Download the feeds page, fill possible info
 * Created by hiep on 25/05/2014.
 */
public class SubscriptionFactory {
	ContentParser contentParser;

	@Inject
	public SubscriptionFactory(ContentParser contentParser) {
		this.contentParser = contentParser;
	}

	public Subscription createSubscription(String feedsUrl, String[] tags, boolean enable, String language, String encoding) throws IOException, FeedParserException {
		Feeds feeds = contentParser.parseFeeds(feedsUrl, encoding);
		String feedsLang =  feeds.getLanguage();
		if (Strings.isNullOrEmpty(feedsLang)) {
			feedsLang = language;
		}

		//DateTime.now().toString(ISODateTimeFormat.dateTime())
		return new Subscription(null,feedsUrl, getTechnicalTags(tags), feeds.getDescription(), feedsLang, enable, encoding, feeds.getPubDate(), DateTime.now().toDate());
	}
	public Subscription createSubscription(String feedsUrl, String[] tags, boolean enable, String language) throws IOException, FeedParserException {
		return  createSubscription(feedsUrl, tags, enable, language, Constants.DEFAULT_ENCODING);
	}
	public Subscription createSubscription(String feedsUrl, String[] tags, boolean enable) throws IOException, FeedParserException {
		return  createSubscription(feedsUrl, tags, enable, null);
	}
	public Subscription createSubscription(String feedsUrl, String[] tags, String language) throws IOException, FeedParserException {
		return  createSubscription(feedsUrl, tags, true, language);
	}
	public Subscription createSubscription(String feedsUrl, String[] tags) throws IOException, FeedParserException {
		return  createSubscription(feedsUrl, tags, true);
	}
	public Subscription createSubscription(String feedsUrl) throws IOException, FeedParserException {
		return  createSubscription(feedsUrl, null);
	}

	private String getTechnicalTags(String[] tags) {
		if (tags==null || tags.length==0) {
			return null;
		}
		StringBuilder sb = new StringBuilder("|");
		for (String tag : tags) {
			sb.append(tag.toUpperCase()+"|");
		}
		return sb.toString();
	}
}
