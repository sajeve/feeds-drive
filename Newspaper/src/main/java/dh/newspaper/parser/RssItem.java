package dh.newspaper.parser;

import java.io.Serializable;

/**
 * Created by hiep on 7/05/2014.
 */
public class RssItem implements Serializable {
	private String title;
	private String publishedDate;
	private String description;
	private String uri;

	/**
	 * optional, can be null
	 */
	public String author;

	public RssItem(String title, String publishedDate, String description, String uri) {
		this.title = title;
		this.publishedDate = publishedDate;
		this.description = description;
		this.uri = uri;
	}

	public String getTitle() {
		return title;
	}

	public String getPublishedDate() {
		return publishedDate;
	}

	public String getDescription() {
		return description;
	}

	public String getUri() {
		return uri;
	}
}
