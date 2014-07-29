package dh.newspaper.event;

import dh.newspaper.adapter.SearchFeedsResultAdapter;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;

import java.io.Serializable;

/**
 * This event is sent when user click on the saveSubscription button
 * Created by hiep on 4/06/2014.
 */
public class SubscribeClickedEvent extends RawEvent implements Serializable {

	SearchFeedsResult.ResponseData.Entry feedsSource;

	public SubscribeClickedEvent(SearchFeedsResult.ResponseData.Entry feedsSource) {
		super("Subscribe", feedsSource.getUrl());
		this.feedsSource = feedsSource;
	}

	public SearchFeedsResult.ResponseData.Entry getFeedsSource() {
		return feedsSource;
	}
}