package dh.newspaper.workflow;

/**
 * Created by hiep on 7/06/2014.
 */
public class WorkflowException extends Exception {
	public WorkflowException(String detailMessage) {
		super(detailMessage);
	}

	public WorkflowException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public WorkflowException(Throwable throwable) {
		super(throwable);
	}
}
