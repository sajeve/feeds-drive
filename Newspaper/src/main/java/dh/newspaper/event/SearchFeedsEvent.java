package dh.newspaper.event;

import dh.newspaper.model.json.SearchFeedsResult;

/**
 * Tell GUI to update search result
 * Created by hiep on 4/06/2014.
 */
public class SearchFeedsEvent<SearchFeedsTask> extends BaseEvent<SearchFeedsTask> {
	private SearchFeedsResult searchResult;
	private Exception error;

	public SearchFeedsEvent(SearchFeedsTask sender, String subject, String flowId) {
		super(sender, subject, flowId);
	}

	public SearchFeedsEvent(SearchFeedsTask sender, String subject, String flowId, SearchFeedsResult searchResult) {
		this(sender, subject, flowId);
		this.searchResult = searchResult;
	}

	public SearchFeedsEvent(SearchFeedsTask sender, String subject, String flowId, Exception error) {
		this(sender, subject, flowId);
		this.error = error;
	}

	public SearchFeedsResult getSearchResult() {
		return searchResult;
	}

	public Exception getError() {
		return error;
	}
}
