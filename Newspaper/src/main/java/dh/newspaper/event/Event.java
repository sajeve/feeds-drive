package dh.newspaper.event;

import com.google.common.base.Strings;

/**
 * Created by hiep on 11/05/2014.
 */
public class Event<T> {
	private T sender;
	private String subject;

	public Event(T sender) {
		this.sender = sender;
	}

	public Event(T sender, String subject) {
		this.sender = sender;
		this.subject = subject;
	}

	public T getSender() {
		return sender;
	}

	public String getSubject() {
		return subject;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(String.format("{Event sender=%s", this.getSender()));
		if (!Strings.isNullOrEmpty(getSubject())) {
			sb.append(" subject='"+this.getSubject()+"'");
		}
		sb.append("}");
		return sb.toString();
	}
}
