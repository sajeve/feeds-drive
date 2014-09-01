package dh.tool.thread.prifo;

import dh.tool.common.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.*;

/**
 * Created by hiep on 26/07/2014.
 */
public class PrifoExecutor extends ThreadPoolExecutor {
	private static final Logger Log = LoggerFactory.getLogger(PrifoExecutor.class);

	public PrifoExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PrifoBlockingQueue workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public PrifoExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PrifoBlockingQueue workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public PrifoExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PrifoBlockingQueue workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public PrifoExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, PrifoBlockingQueue workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	/**
	 * Execute a prifoTask, cancel all other prifo task
	 */
	public void executeUnique(PrifoTask task) {
		try {
			Iterator it = this.getQueue().iterator();

			//cancel all other PrifoTask, except the twin of task
			int count = 0;
			while (it.hasNext()) {
				Object o=it.next();
				if (o instanceof PrifoTask) {
					PrifoTask t = (PrifoTask) o;
					if (!StrUtils.equalsString(t.getMissionId(), task.getMissionId())) {
						t.cancel();
						count++;
						Log.debug(t + " is cancelled by executeUnique - "+getName());
					}
				}
			}
			if (count>0) {
				Log.info(count + " tasks is cancelled by executeUnique - " + getName());
			}
		}
		catch (Exception ex) {
			Log.warn("Failed cancel all prifo task - "+getName(), ex);
		}

		task.setFocus(true);
		execute(task);
	}

	/**
	 * Cancel all prifo task
	 */
	public void cancelAll() {
		int count = 0;
		Iterator it = this.getQueue().iterator();
		while (it.hasNext()) {
			Object o=it.next();
			if (o instanceof PrifoTask) {
				((PrifoTask)o).cancel();
				count++;
			}
		}
		Log.info(String.format("CancelAll %d / %d - %s", count, this.getQueue().size(), getName()));
	}

	public String getName() {
		return ((PrifoBlockingQueue)getQueue()).getName();
	}

	public IQueueEmptyCallback getQueueEmptyCallback() {
		return  ((PrifoBlockingQueue)getQueue()).getQueueEmptyCallback();
	}
	public void setQueueEmptyCallback(IQueueEmptyCallback queueEmptyCallback) {
		((PrifoBlockingQueue)getQueue()).setQueueEmptyCallback(queueEmptyCallback);
	}
}
