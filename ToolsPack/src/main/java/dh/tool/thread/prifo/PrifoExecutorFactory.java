package dh.tool.thread.prifo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 12/06/2014.
 */
public class PrifoExecutorFactory {
	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize, int maxPoolSize, IQueueEmptyCallback queueEmptyCallback) {
		return new PrifoExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new PrifoBlockingQueue(name, queueEmptyCallback));
	}
	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize) {
		return newPrifoExecutor(name, corePoolSize, Integer.MAX_VALUE, null);
	}
	public static PrifoExecutor newPrifoExecutor(String name) {
		return newPrifoExecutor(name, 0);
	}
}
