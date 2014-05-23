package dh.newspaper.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hiep on 15/05/2014.
 */
public class JsonSubscription {
	private String feedsUrl;
	private List<Long> categories = Arrays.asList(0L);
	private boolean enable = true;
	private String language;

	public JsonSubscription() {
	}

	public JsonSubscription(String feedsUrl, List<Long> categories, boolean enable, String language) {
		this.feedsUrl = feedsUrl;
		this.categories = categories;
		this.enable = enable;
		this.language = language;
	}
	public JsonSubscription(String feedsUrl, List<Long> categories, String language) {
		this(feedsUrl, categories, true, language);
	}
	public JsonSubscription(String feedsUrl, List<Long> categories) {
		this(feedsUrl, categories, true, null);
	}
	public JsonSubscription(String feedsUrl) {
		this(feedsUrl, Arrays.asList(0L));
	}

	@JsonProperty(required = true)
	public String getFeedsUrl() {
		return feedsUrl;
	}

	public List<Long> getCategories() {
		return categories;
	}

	public boolean isEnable() {
		return enable;
	}

	public String getLanguage() {
		return language;
	}

	public void setFeedsUrl(String feedsUrl) {
		this.feedsUrl = feedsUrl;
	}

	public void setCategories(List<Long> categories) {
		this.categories = categories;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
