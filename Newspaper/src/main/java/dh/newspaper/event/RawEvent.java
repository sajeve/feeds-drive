package dh.newspaper.event;

import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * A running workflow emit many Event which has the same flowId but might have different subjects
 * - Subject often describe an action: StartCompute, UpdateResult, DoneCompute..
 * Created by hiep on 28/07/2014.
 */
public class RawEvent implements Serializable {
	private String flowId;
	private String subject;

	public RawEvent() {
	}

	public RawEvent(String subject) {
		this.subject = subject;
	}

	public RawEvent(String subject, String flowId) {
		this.subject = subject;
		this.flowId = flowId;
	}

	public String getSubject() {
		return subject;
	}

	public String getFlowId() {
		return flowId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[Event ");
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
