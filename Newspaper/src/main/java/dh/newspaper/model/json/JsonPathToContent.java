package dh.newspaper.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Created by hiep on 23/05/2014.
 */
public class JsonPathToContent implements Serializable {
	private String urlPattern;
	private String xpath;
	private String language;
	private Integer priority=0;
	private boolean enable = true;

	public JsonPathToContent() {
	}

	public JsonPathToContent(String urlPattern, String xpath, String language, Boolean enable) {
		this.urlPattern = urlPattern;
		this.xpath = xpath;
		this.language = language;
		this.enable = enable;
	}
	public JsonPathToContent(String urlPattern, String xpath, String language) {
		this(urlPattern, xpath, language, true);
	}
	public JsonPathToContent(String urlPattern, String xpath) {
		this(urlPattern, xpath, null);
	}

	@JsonProperty(required = true)
	public String getUrlPattern() {
		return urlPattern;
	}

	@JsonProperty(required = true)
	public String getXpath() {
		return xpath;
	}

	public String getLanguage() {
		return language;
	}

	public boolean isEnable() {
		return enable;
	}

	public Integer getPriority() {
		return priority;
	}

	public void setUrlPattern(String urlPattern) {
		this.urlPattern = urlPattern;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}
}
