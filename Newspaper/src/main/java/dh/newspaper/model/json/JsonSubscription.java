//package dh.newspaper.model.json;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//
//import java.util.Arrays;
//import java.util.List;
//
///**
// * Created by hiep on 15/05/2014.
// */
//public class JsonSubscription {
//	private String feedsUrl;
//	private List<String> tags;
//	private boolean enable = true;
//	private String language;
//	private String encoding = "UTF-8";
//
//	public JsonSubscription() {
//	}
//
//	public JsonSubscription(String feedsUrl, List<String> tags, boolean enable, String language, String encoding) {
//		this.feedsUrl = feedsUrl;
//		this.tags = tags;
//		this.enable = enable;
//		this.language = language;
//		this.encoding = encoding;
//	}
//
//	public String getFeedsUrl() {
//		return feedsUrl;
//	}
//
//	public List<String> getTags() {
//		return tags;
//	}
//
//	public boolean isEnable() {
//		return enable;
//	}
//
//	public String getLanguage() {
//		return language;
//	}
//
//	public String getEncoding() {
//		return encoding;
//	}
//
//	public void setFeedsUrl(String feedsUrl) {
//		this.feedsUrl = feedsUrl;
//	}
//
//	public void setTags(List<String> tags) {
//		this.tags = tags;
//	}
//
//	public void setEnable(boolean enable) {
//		this.enable = enable;
//	}
//
//	public void setLanguage(String language) {
//		this.language = language;
//	}
//
//	public void setEncoding(String encoding) {
//		this.encoding = encoding;
//	}
//}
