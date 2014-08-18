package dh.tool.thread.prifo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 12/06/2014.
 */
public class PrifoExecutorFactory {
	public static PrifoExecutor newPrifoExecutor(int corePoolSize, int maxPoolSize, IQueueEmptyCallback queueEmptyCallback) {
		return new PrifoExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new PrifoBlockingQueue(queueEmptyCallback));
	}
	public static PrifoExecutor newPrifoExecutor(int corePoolSize, int maxPoolSize) {
		return newPrifoExecutor(corePoolSize, maxPoolSize, null);
	}
	public static PrifoExecutor newPrifoExecutor() {
		return newPrifoExecutor(0, Integer.MAX_VALUE);
	}
}
