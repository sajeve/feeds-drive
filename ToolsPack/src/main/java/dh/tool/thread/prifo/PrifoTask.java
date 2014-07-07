package dh.tool.thread.prifo;

/**
 * A {@link PrifoTask} must implement {@link #run()} which constantly check for {@link #isCancelled()}
 * so that the task can properly terminate as soon as the {@link #cancel()} is called.
 * <p></p>
 * Overriding the {@link #cancel()} method must call super.cancel(), (do it to cancel other sub tasks or to
 * free up other resources).
 * <p></p>
 *
 * A {@link PrifoTask} must also implement missionId. Two task of the same missionId are twin
 * if we add task in the queue which already contains the twin so the task will not be added, but the priority of the twin
 * will be increased. See {@link PrifoQueue#offer(IPrifosable)}
 *
 * Created by hiep on 12/06/2014.
 */
public abstract class PrifoTask implements IPrifosable, Comparable {
//	private static final String TAG = PrifoTask.class.getName();

	private int priority;
	private boolean focused;
	private volatile boolean cancelled = false;

//	private final Stopwatch insideSw = Stopwatch.createUnstarted();

	@Override
	public int getPriority() {
		return priority;
	}
	@Override
	public IPrifosable increasePriority() {
		this.priority++;
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
	public void onEnterQueue(PrifoQueue queue) {
//		try {
//			if (Constants.DEBUG) {
//				insideSw.start();
//			}
//		}
//		catch (Exception ex) {
//			Log.w(TAG, ex);
//		}
	}
	@Override
	public void onDequeue(PrifoQueue queue) {
//		try {
//			if (Constants.DEBUG) {
//				Log.v(TAG, String.format("Poll (inQueue=%d ms, priority=%d%s) N=%d - %s",
//						insideSw.elapsed(TimeUnit.MILLISECONDS),
//						getPriority(),
//						isFocused() ? " focused" : "",
//						queue.size(), this.getMissionId()));
//			}
//		}
//		catch (Exception ex) {
//			Log.w(TAG, ex);
//		}
	}

	@Override
	public boolean isCancelled() {
		return cancelled || Thread.interrupted();
	}

	public void cancel() {
		cancelled = true;
	}

	/**
	 * Use to recognise if the two task has the same mission,
	 * in {@link PrifoExecutors} if we add 2 task of the same id (a twin) into the queue, it will not
	 * add the task but increase the priority of the existing twin-task in the queue.
	 */
	public abstract String getMissionId();

	@Override
	public int compareTo(Object another) {
		if (another==null) {
			return -1000;
		}
		PrifoTask other = (PrifoTask)another;

		if (this.isFocused() && !other.isFocused()) {
			return Integer.MIN_VALUE;
		}
		if (!this.isFocused() && other.isFocused()) {
			return Integer.MAX_VALUE;
		}

		if (other.getPriority() == this.getPriority()) {
			return this.getMissionId().compareTo(other.getMissionId());
		}
		return other.getPriority()-this.getPriority();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || ((Object)this).getClass() != (Object)o.getClass()) return false;

		PrifoTask prifoTask = (PrifoTask) o;

		if (!getMissionId().equals(prifoTask.getMissionId())) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return getMissionId().hashCode();
	}
}
