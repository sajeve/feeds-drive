package dh.newspaper.model;

import java.io.Serializable;

/**
 * Created by hiep on 7/05/2014.
 */
public class FeedItem implements Serializable {
	private String title;
	private String publishedDate;
	private String description;
	private String uri;
	private String language;

	/**
	 * optional, can be null
	 */
	public String author;

	public FeedItem(String title, String publishedDate, String description, String uri, String language) {
		this.title = title;
		this.publishedDate = publishedDate;
		this.description = description;
		this.uri = uri;
		this.language = language;
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

	public String getLanguage() {
		return language;
	}
}
