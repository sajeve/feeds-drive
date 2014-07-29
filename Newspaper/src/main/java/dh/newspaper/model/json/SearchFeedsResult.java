package dh.newspaper.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.Subscription;
import dh.tool.common.StrUtils;

import java.io.Serializable;
import java.util.List;

/**
 * The json structure of search result came from Google API
 * with some more attribute, to hold view state.
 * Created by hiep on 22/07/2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchFeedsResult implements Serializable {
	private ResponseData responseData;
	private String responseDetails;
	private Integer responseStatus;

	public ResponseData getResponseData() {
		return responseData;
	}

	public void setResponseData(ResponseData responseData) {
		this.responseData = responseData;
	}

	public String getResponseDetails() {
		return responseDetails;
	}

	public void setResponseDetails(String responseDetails) {
		this.responseDetails = responseDetails;
	}

	public Integer getResponseStatus() {
		return responseStatus;
	}

	public void setResponseStatus(Integer responseStatus) {
		this.responseStatus = responseStatus;
	}

	@JsonIgnore
	public int count() {
		if (this.getResponseData() == null) {
			return 0;
		}
		if (this.getResponseData().getEntries() == null) {
			return 0;
		}
		return this.getResponseData().getEntries().size();
	}

	@JsonIgnore
	public ResponseData.Entry getItem(int position) {
		if (this.getResponseData() == null) {
			return null;
		}
		if (this.getResponseData().getEntries() == null) {
			return null;
		}
		return this.getResponseData().getEntries().get(position);
	}

	public static class ResponseData implements Serializable {
		private List<Entry> entries;
		private String query;

		public List<Entry> getEntries() {
			return entries;
		}

		public void setEntries(List<Entry> entries) {
			this.entries = entries;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		/**
		 * Hold info about a feed source. The {@link #subscription} won't null
		 * if the feed source had been already subscribed.
		 *
		 * The validity is checked (true) if the page (feeds source) is successfully parsed
		 */
		public static class Entry implements Serializable {
			private String contentSnippet;
			/**
			 * Website url
			 */
			private String link;
			private String title;
			/**
			 * Feeds Source URL
			 */
			private String url;
			private FeedsSourceValidity validity = FeedsSourceValidity.UNKNOWN;
			private Subscription subscription;
			private Feeds feeds;

			public String getContentSnippet() {
				return contentSnippet;
			}

			public void setContentSnippet(String contentSnippet) {
				this.contentSnippet = contentSnippet;
			}

			public String getLink() {
				return link;
			}

			public void setLink(String link) {
				this.link = link;
			}

			public String getTitle() {
				return title;
			}

			public void setTitle(String title) {
				this.title = title;
			}

			/**
			 * URL of Feeds Source
			 */
			public String getUrl() {
				return url;
			}

			public void setUrl(String url) {
				this.url = StrUtils.removeTrailingSlash(url);
			}

			@JsonIgnore
			public Subscription getSubscription() {
				return subscription;
			}

			@JsonIgnore
			public void setSubscription(Subscription subscription) {
				this.subscription = subscription;
			}

			@JsonIgnore
			public FeedsSourceValidity getValidity() {
				return subscription!=null ? FeedsSourceValidity.OK : validity;
			}

			@JsonIgnore
			public void setValidity(FeedsSourceValidity validity) {
				this.validity = validity;
			}
			@JsonIgnore
			public Feeds getFeeds() {
				return feeds;
			}
			@JsonIgnore
			public void setFeeds(Feeds feeds) {
				this.feeds = feeds;
			}
		}
	}
	public static enum FeedsSourceValidity {OK, KO, UNKNOWN};
}
