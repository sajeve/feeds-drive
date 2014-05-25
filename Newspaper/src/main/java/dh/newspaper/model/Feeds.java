package dh.newspaper.model;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by hiep on 25/05/2014.
 */
public class Feeds extends ArrayList<FeedItem> {
	private final String url;
	private final String language;
	private final String description;
	private final String pubDate;

	public Feeds(int capacity, String url, String language, String description, String pubDate) {
		super(capacity);
		this.url = url;
		this.language = language;
		this.description = description;
		this.pubDate = pubDate;
	}

	public Feeds(String url, String language, String description, String pubDate) {
		this.url = url;
		this.language = language;
		this.description = description;
		this.pubDate = pubDate;
	}

	public Feeds(Collection<? extends FeedItem> collection, String url, String language, String description, String pubDate) {
		super(collection);
		this.url = url;
		this.language = language;
		this.description = description;
		this.pubDate = pubDate;
	}

	public String getLanguage() {
		return language;
	}

	public String getDescription() {
		return description;
	}

	public String getPubDate() {
		return pubDate;
	}

	public String getUrl() {
		return url;
	}
}
