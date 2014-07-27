package dh.newspaper.event;

import dh.newspaper.adapter.SearchFeedsResultAdapter;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;

/**
 * This event is sent when user click on the subscribe button
 * Created by hiep on 4/06/2014.
 */
public class SubscribeClickedEvent<SearchFeedsResultAdapter> extends BaseEvent<SearchFeedsResultAdapter> {

	SearchFeedsResult.ResponseData.Entry feedsSource;

	public SubscribeClickedEvent(SearchFeedsResultAdapter sender, SearchFeedsResult.ResponseData.Entry feedsSource) {
		super(sender, "Subscribe", feedsSource.getUrl());
		this.feedsSource = feedsSource;
	}

	public SearchFeedsResult.ResponseData.Entry getFeedsSource() {
		return feedsSource;
	}
}