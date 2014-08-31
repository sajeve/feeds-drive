package dh.newspaper.workflow;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SaveSubscriptionEvent;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.DaoMaster;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.tools.TagUtils;
import dh.tool.common.StrUtils;
import dh.tool.thread.prifo.OncePrifoTask;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CancellationException;

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
	//@Inject DaoSession daoSession;
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

	//private DaoSession daoSession;

	public void perform() {
		DaoMaster daoMaster = refData.createWritableDaoMaster();
		try {
			DaoSession daoSession = daoMaster.newSession();

			String feedsSourceUrl = StrUtils.removeTrailingSlash(feedsSource.getUrl());

			if (feedsSource.getValidity() == SearchFeedsResult.FeedsSourceValidity.UNKNOWN) {
				sendProgressMessage("Checking feeds source validity: downloading.."); //TODO: translate

				//check validity of the feed source
				String input = NetworkUtils.quickDownloadXml(feedsSourceUrl, NetworkUtils.DESKTOP_USER_AGENT, this);

				if (TextUtils.isEmpty(input)) {
					sendError("Feeds source is not valid"); //TODO: translate
					return;
				}

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
				String tagsValue = null;
				if (tags!=null && !tags.isEmpty()){
					tagsValue = TagUtils.getTechnicalTags(tags);
				}

				Subscription existedSubscription = feedsSource.getSubscription();
				if (existedSubscription!=null) {
					if (TextUtils.isEmpty(tagsValue)) {
						existedSubscription.setEnable(false);
					}
					else {
						existedSubscription.setEnable(true);
					}
					existedSubscription.setTags(tagsValue);
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
					if (TextUtils.isEmpty(tagsValue)) {
						throw new IllegalStateException("tags value cannot empty here");
					}
					daoSession.insert(new Subscription(null, feedsSourceUrl, tagsValue,
							description, language, true, null, pubDate, DateTime.now().toDate()));
				}

				sendProgressMessage("updating cache..."); //TODO: translate
				refData.loadSubscriptionAndTags();

				sendDone();
			}
			else {
				sendError("Feeds source is not valid"); //TODO: translate
			}
		} catch (CancellationException e) {
			throw e;
		}catch (Exception ex) {
			sendError("Failed saving subscription: " + ex); //TODO: translate
			Log.w(TAG, ex);
		}
		finally {
			daoMaster.getDatabase().close();
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
