//package dh.newspaper.event;
//
//import dh.newspaper.Constants;
//import dh.newspaper.adapter.SearchFeedsResultAdapter;
//import dh.newspaper.model.generated.Subscription;
//import dh.newspaper.model.json.SearchFeedsResult;
//
//import java.io.Serializable;
//
///**
// * This event is sent when user click on the saveSubscription button
// * Created by hiep on 4/06/2014.
// */
//public class SubscribeClickedEvent extends RawEvent implements Serializable {
//	private static final String SUBJECT_CLICK = "Subscribe";
//	//public static final String SUBJECT_REFRESH = "Refresh";
//
//	SearchFeedsResult.ResponseData.Entry feedsSource;
//
//	public SubscribeClickedEvent(SearchFeedsResult.ResponseData.Entry feedsSource) {
//		super(SUBJECT_CLICK, feedsSource.getUrl());
//		this.feedsSource = feedsSource;
//	}
//
//	public SearchFeedsResult.ResponseData.Entry getFeedsSource() {
//		return feedsSource;
//	}
//}