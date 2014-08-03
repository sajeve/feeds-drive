package dh.newspaper.workflow;

import android.content.Context;
import android.util.Log;
import com.google.common.base.Joiner;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SaveSubscriptionEvent;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.OncePrifoTask;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.inject.Inject;
import java.util.Set;

/**
* Created by hiep on 28/07/2014.
*/
public class SaveSubscriptionWorkflow extends OncePrifoTask {
	private final static String TAG = SaveSubscriptionWorkflow.class.getName();
	private final SearchFeedsResult.ResponseData.Entry feedsSource;
	private final Set<String> tags;
	//private final Context context;
	private SaveSubscriptionEvent saveSubscriptionState;
	@Inject ContentParser contentParser;
	@Inject DaoSession daoSession;
	@Inject RefData refData;

	public SaveSubscriptionWorkflow(Context context, SearchFeedsResult.ResponseData.Entry feedsSource, Set<String> tags) {
		((MyApplication)context.getApplicationContext()).getObjectGraph().inject(this);
		this.feedsSource = feedsSource;
		this.tags = tags;
		//this.context = context;
	}

	@Override
	public String getMissionId() {
		return feedsSource.getUrl();
	}

//	@Override
//	public void perform() {
//		try {
//			String feedsSourceUrl = feedsSource.getUrl();
//
//			sendProgressMessage("Checking feeds source validity: downloading.."+feedsSourceUrl); //TODO: translate
//			Thread.sleep(2000);
//			sendProgressMessage("Checking feeds source validity: parsing.."+feedsSourceUrl); //TODO: translate
//			Thread.sleep(1000);
//			sendProgressMessage("Checking feeds source validity: parse OK"+feedsSourceUrl); //TODO: translate
//			Thread.sleep(1000);
//			sendProgressMessage("Saving subscription..."+feedsSourceUrl); //TODO: translate
//			Thread.sleep(1000);
//			sendDone();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}

	public void perform() {
		try {
			String feedsSourceUrl = StrUtils.removeTrailingSlash(feedsSource.getUrl());

			if (feedsSource.getValidity() == SearchFeedsResult.FeedsSourceValidity.UNKNOWN) {
				sendProgressMessage("Checking feeds source validity: downloading.."); //TODO: translate

				//check validity of the feed source
				String input = NetworkUtils.quickDownloadXml(feedsSourceUrl, NetworkUtils.DESKTOP_USER_AGENT, this);

				sendProgressMessage("Checking feeds source validity: parsing.."); //TODO: translate
				Document doc = Jsoup.parse(input, feedsSourceUrl, Parser.xmlParser());
				Feeds feeds = contentParser.parseFeeds(doc, this);

				if (feeds.size()>1) {
					sendProgressMessage("Checking feeds source validity: parse OK"); //TODO: translate

					feedsSource.setValidity(SearchFeedsResult.FeedsSourceValidity.OK);
					feedsSource.setFeeds(feeds);
				}
				else {
					feedsSource.setValidity(SearchFeedsResult.FeedsSourceValidity.KO);
				}
			}

			if (feedsSource.getValidity()== SearchFeedsResult.FeedsSourceValidity.OK) {
				sendProgressMessage("Saving subscription..."); //TODO: translate

				StringBuilder tagsValue = new StringBuilder("|");
				for (String t : tags) {
					tagsValue.append(t.toUpperCase()).append("|");
				}

				Subscription existedSubscription = feedsSource.getSubscription();

				if (existedSubscription!=null) {
					existedSubscription.setTags(tagsValue.toString());
					existedSubscription.setLastUpdate(DateTime.now().toDate());
					daoSession.getSubscriptionDao().update(existedSubscription);
				}
				else {
					String description = feedsSource.getContentSnippet();
					String language = null;
					String pubDate = null;
					if (feedsSource.getFeeds()!=null) {
						description = feedsSource.getFeeds().getDescription();
						language = feedsSource.getFeeds().getLanguage();
						pubDate = feedsSource.getFeeds().getPubDate();
					}
					daoSession.insert(new Subscription(null, feedsSourceUrl, tagsValue.toString(),
							description, language, true, null, pubDate, DateTime.now().toDate()));
				}

				sendProgressMessage("update cache..."); //TODO: translate
				refData.loadSubscriptions();

				sendDone();
			}
			else {
				sendError("Feeds source not valid"); //TODO: translate
			}
		}catch (Exception ex) {
			sendError("Failed saving subscription: " + ex.toString()); //TODO: translate
			Log.w(TAG, ex);
		}
	}

	public SaveSubscriptionEvent getSaveSubscriptionState() {
		return saveSubscriptionState;
	}

	private void sendProgressMessage(String message) {
		saveSubscriptionState = new SaveSubscriptionEvent(
				Constants.SUBJECT_SAVE_SUBSCRIPTION_PROGRESS_MESSAGE,
				feedsSource.getUrl(),
				message);
		EventBus.getDefault().post(saveSubscriptionState);
	}
	private void sendError(String message) {
		saveSubscriptionState = new SaveSubscriptionEvent(
				Constants.SUBJECT_SAVE_SUBSCRIPTION_ERROR,
				feedsSource.getUrl(),
				message);
		EventBus.getDefault().post(saveSubscriptionState);
	}
	private void sendDone() {
		saveSubscriptionState = new SaveSubscriptionEvent(
				Constants.SUBJECT_SAVE_SUBSCRIPTION_DONE,
				feedsSource.getUrl());
		EventBus.getDefault().post(saveSubscriptionState);
	}

}
