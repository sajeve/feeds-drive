package dh.newspaper.event;

/**
 * Created by hiep on 27/07/2014.
 */
public class CreateNewTagEvent extends BaseEvent {
	public CreateNewTagEvent(Object sender, String subject, String flowId) {
		super(sender, subject, flowId);
	}

	public CreateNewTagEvent(Object sender) {
		super(sender);
	}

	public CreateNewTagEvent(Object sender, String subject) {
		super(sender, subject);
	}
}
