package dh.newspaper.tools;

import com.google.common.base.Stopwatch;
import org.joda.time.DateTime;

/**
 * Created by hiep on 9/06/2014.
 */
public abstract class BumpTask implements Runnable, Comparable {
	private int priority;
	private boolean active;
	private final Stopwatch outsideSw = Stopwatch.createStarted();
	private final Stopwatch insideSw = Stopwatch.createUnstarted();

	public boolean isActive() {
		return active;
	}

	public BumpTask setActive(boolean active) {
		this.active = active;
		return this;
	}

	public int getPriority() {
		return priority;
	}

	/**
	 * increase the task priority
	 */
	public BumpTask hit() {
		this.priority++;
		return this;
	}

	/**
	 * Id is use to recognise if the two task has the same mission,
	 * in {@link dh.newspaper.tools.BumpBlockingQueue} if we add 2 task of the same id (a twin) into the queue, it will not
	 * add the task but increase the priority of the existing twin-task in the queue.
	 */
	public abstract String getId();

	@Override
	public int compareTo(Object another) {
		if (another==null) {
			return -1000;
		}
		BumpTask other = (BumpTask)another;

		if (this.isActive() && !other.isActive()) {
			return Integer.MIN_VALUE;
		}
		if (!this.isActive() && other.isActive()) {
			return Integer.MAX_VALUE;
		}
		return other.getPriority()-this.getPriority();
	}

	void enterQueue() {
		outsideSw.stop();
		insideSw.start();
	}
	public Stopwatch getOutsideSw() {
		return outsideSw;
	}
	public Stopwatch getInsideSw() {
		return insideSw;
	}
}
