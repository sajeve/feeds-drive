package dh.newspaper.event;

import dh.newspaper.adapter.SearchFeedsResultAdapter;
import dh.newspaper.model.json.SearchFeedsResult;

/**
 * Tell GUI to update search result
 * Created by hiep on 4/06/2014.
 */
public class SubscribeClickedEvent<SearchFeedsResultAdapter> extends BaseEvent<SearchFeedsResultAdapter> {
	public SubscribeClickedEvent(SearchFeedsResultAdapter sender, String feedUrl) {
		super(sender, "Subscribe", feedUrl);
	}

	public String getFeedUrl() {
		return getFlowId();
	}
}
