package dh.newspaper.event;

import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * Created by hiep on 11/05/2014.
 */
public class BaseEvent<T> {
	private String flowId;
	private T sender;
	private String subject;


	public BaseEvent(T sender, String subject, String flowId) {
		if (sender == null) {
			throw new IllegalStateException("Null event sender");
		}
		this.sender = sender;
		this.subject = subject;
		this.flowId = flowId;
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

	public String getSubject() {
		return subject;
	}

	public String getFlowId() {
		return flowId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[Event sender=" + this.getSender());
		if (!Strings.isNullOrEmpty(getSubject())) {
			sb.append(" subject='"+this.getSubject()+"'");
		}
		sb.append("]");
		return sb.toString();
	}
}
