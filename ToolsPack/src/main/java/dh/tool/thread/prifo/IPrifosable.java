package dh.tool.thread.prifo;

import dh.tool.thread.ICancellation;

/**
 * A task must implement this interface to use in {@link PrifoBlockingQueue}
 * two {@link IPrifosable} tasks with the same {@link #getMissionId()} are twin
 * if we add task in the queue which already contains the twin so the later task will not be added with a higher priority
 * and its twin will be remove from the database
 *
 * Created by hiep on 12/06/2014.
 */
public interface IPrifosable extends Runnable, ICancellation {
	public int getPriority();
	public IPrifosable setPriority(int p);
	public IPrifosable setFocus(boolean focused);
	public boolean isFocused();
	public void onEnterQueue(PrifoQueue queue);
	public void onDequeue(PrifoQueue queue);

	/**
	 * Use to recognise if the two task has the same mission,
	 * in {@link PrifoExecutorFactory} if we add 2 task of the same id (a twin) into the queue, it will not
	 * add the task but increase the priority of the existing twin-task in the queue.
	 */
	public String getMissionId();
}
