package dh.newspaper.event;

import dh.newspaper.model.json.SearchFeedsResult;

import java.io.Serializable;

/**
 * This event is sent when user click on the saveSubscription button
 * Created by hiep on 4/06/2014.
 */
public class SaveSubscriptionEvent extends RawEvent implements Serializable {
	private String progressMessage;

	public SaveSubscriptionEvent(String subject, String flowId, String progressMessage) {
		super(subject, flowId);
		this.progressMessage = progressMessage;
	}

	public SaveSubscriptionEvent(String subject, String flowId) {
		super(subject, flowId);
	}

	public SaveSubscriptionEvent() {
		super();
	}

	public SaveSubscriptionEvent(String subject) {
		super(subject);
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}
}