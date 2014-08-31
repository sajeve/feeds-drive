package dh.tool.thread.prifo;

import dh.tool.thread.ICancellation;

import java.util.concurrent.CancellationException;

/**
 * A {@link PrifoTask} must implement {@link #run()} which constantly call {@link #checkCancellation}
 * so that the task can properly terminate as soon as the {@link #cancel()} is called.
 * <p></p>
 * Overriding the {@link #cancel()} method MUST to call super.cancel(), (do it to cancel other sub tasks or to
 * free up other resources).
 * <p></p>
 *
 * will be increased. See {@link PrifoQueue#offer(IPrifosable)}
 *
 * Created by hiep on 12/06/2014.
 */
public abstract class PrifoTask implements IPrifosable {
	//private static final String TAG = PrifoTask.class.getName();
	//private static final Logger log = LoggerFactory.getLogger(PrifoTask.class);

	private volatile int priority;
	private volatile boolean focused;
	private volatile boolean cancelled = false;

	//private PerfWatcher pw;

	@Override
	public int getPriority() {
		return priority;
	}
	@Override
	public IPrifosable setPriority(int p) {
		this.priority = p;
		return this;
	}
	@Override
	public IPrifosable setFocus(boolean focused) {
		this.focused = focused;
		return this;
	}
	@Override
	public boolean isFocused() {
		return this.focused;
	}
	@Override
	public boolean isCancelled() {
		return cancelled || Thread.interrupted();
	}
	public void cancel() {
		cancelled = true;
	}
	public void checkCancellation() {
		if (isCancelled()) {
			throw new CancellationException();
		}
	}
	public void checkCancellation(String message) {
		if (isCancelled()) {
			throw new CancellationException(message);
		}
	}
//	@Override
//	public int compareTo(Object another) {
//		if (another==null) {
//			return -1000;
//		}
//		PrifoTask other = (PrifoTask)another;
//
//		if (this.isFocused() && !other.isFocused()) {
//			return Integer.MIN_VALUE;
//		}
//		if (!this.isFocused() && other.isFocused()) {
//			return Integer.MAX_VALUE;
//		}
//
//		/*if (other.getPriority() == this.getPriority()) {
//			return this.getMissionId().compareTo(other.getMissionId());
//		}*/
//		return other.getPriority()-this.getPriority();
//	}
//
//	@Override
//	public boolean equals(Object o) {
//		if (this == o) return true;
//		if (o == null || ((Object)this).getClass() != (Object)o.getClass()) return false;
//
//		PrifoTask prifoTask = (PrifoTask) o;
//
//		if (!getMissionId().equals(prifoTask.getMissionId())) return false;
//
//		return true;
//	}
//
//	@Override
//	public int hashCode() {
//		return getMissionId().hashCode();
//	}
}
