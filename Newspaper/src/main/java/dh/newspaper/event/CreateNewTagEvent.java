package dh.newspaper.event;

import java.io.Serializable;

/**
 * Created by hiep on 27/07/2014.
 */
public class CreateNewTagEvent extends RawEvent implements Serializable {
	public CreateNewTagEvent(String subject, String flowId) {
		super(subject, flowId);
	}

	public CreateNewTagEvent() {
		super();
	}

	public CreateNewTagEvent(String subject) {
		super(subject);
	}
}
