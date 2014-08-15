package dh.newspaper.parser;

import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.tools.TagUtils;
import dh.tool.common.StrUtils;
import dh.tool.thread.ICancellation;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Download the feeds page, fill possible info
 * Created by hiep on 25/05/2014.
 */
public class SubscriptionFactory {
	private ContentParser mContentParser;

	@Inject
	public SubscriptionFactory(ContentParser contentParser) {
		this.mContentParser = contentParser;
	}

	public Subscription createSubscription(String feedsUrl, String[] tags, boolean enable, String language, String encoding) throws IOException, FeedParserException {
		Feeds feeds = mContentParser.parseFeeds(feedsUrl, encoding, new ICancellation() {
			@Override
			public boolean isCancelled() {
				return false;
			}
		});
		String feedsLang =  feeds.getLanguage();
		if (Strings.isNullOrEmpty(feedsLang)) {
			feedsLang = language;
		}

		//DateTime.now().toString(ISODateTimeFormat.dateTime())
		return new Subscription(null,feedsUrl, TagUtils.getTechnicalTags(tags), feeds.getDescription(), feedsLang, enable, encoding, feeds.getPubDate(), DateTime.now().toDate());
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


}
