package dh.newspaper.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dh.newspaper.model.generated.Subscription;

import java.util.List;

/**
 * Created by hiep on 22/07/2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchFeedsResult {
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

	public static class ResponseData {
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

		public static class Entry {
			private String contentSnippet;
			private String link;
			private String title;
			private String url;
			private Subscription subscription;

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

			public String getUrl() {
				return url;
			}

			public void setUrl(String url) {
				this.url = url;
			}

			@JsonIgnore
			public Subscription getSubscription() {
				return subscription;
			}

			@JsonIgnore
			public void setSubscription(Subscription subscription) {
				this.subscription = subscription;
			}
		}
	}
}
