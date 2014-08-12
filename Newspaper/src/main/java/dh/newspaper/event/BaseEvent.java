package dh.newspaper.event;

import com.google.common.base.Strings;

import java.io.Serializable;
import java.security.InvalidParameterException;

/**
 * A context aware version of RawEvent, hold reference to the sender.
 * This one is not serializable so, don't use it to persist Activity state
 * Created by hiep on 11/05/2014.
 */
public class BaseEvent<T> extends RawEvent {
	private T sender;

	public BaseEvent(T sender, String subject, String flowId) {
		super(subject, flowId);
		if (sender == null) {
			throw new InvalidParameterException("Null event sender");
		}
		this.sender = sender;
	}

	public BaseEvent(T sender) {
		this(sender, null, null);
	}

	public BaseEvent(T sender, String subject) {
		this(sender, subject, null);
	}

	public T getSender() {
		return sender;
	}

	@Override
	public String toString() {
		String s = this.getClass().getSimpleName();
		StringBuilder sb = new StringBuilder("["+s+" sender=" + this.getSender());
		if (!Strings.isNullOrEmpty(getSubject())) {
			sb.append(" subject='"+this.getSubject()+"'");
		}
		if (!Strings.isNullOrEmpty(getFlowId())) {
			sb.append(" flowId='" + this.getFlowId() + "'");
		}
		sb.append("]");
		return sb.toString();
	}
}
