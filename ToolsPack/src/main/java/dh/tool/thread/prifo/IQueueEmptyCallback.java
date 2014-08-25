package dh.tool.thread.prifo;

/**
 * Callback is raised when a PrifoQueue is empty.
 * it means that all the task has been "probably" finished (still not sure)
 *
 * WARNING: Implementation of this callback must be very short/quick operation,
 * because it will block the queue.
 *
 * The typical scenario is to log a message "Waiting queue is empty, all tasks seem finished"
 *
 * Created by hiep on 3/08/2014.
 */
public interface IQueueEmptyCallback {
	public void onQueueEmpty(String queueName);
}
