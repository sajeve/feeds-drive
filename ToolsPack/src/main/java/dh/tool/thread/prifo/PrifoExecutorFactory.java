package dh.tool.thread.prifo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hiep on 12/06/2014.
 */
public class PrifoExecutorFactory {
	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize, int maxPoolSize, PrifoBlockingQueue queue) {
		return new PrifoExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, queue, new NamedThreadFactory(name));
	}
	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize, PrifoBlockingQueue queue) {
		return newPrifoExecutor(name, corePoolSize, Integer.MAX_VALUE, queue);
	}
	public static PrifoExecutor newPrifoExecutor(String name, PrifoBlockingQueue queue) {
		return newPrifoExecutor(name, 0, queue);
	}

	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize, int maxPoolSize, IQueueEmptyCallback queueEmptyCallback) {
		return new PrifoExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new PrifoBlockingQueue(name, queueEmptyCallback), new NamedThreadFactory(name));
	}
	public static PrifoExecutor newPrifoExecutor(String name, int corePoolSize) {
		return newPrifoExecutor(name, corePoolSize, Integer.MAX_VALUE, (IQueueEmptyCallback)null);
	}
	public static PrifoExecutor newPrifoExecutor(String name) {
		return newPrifoExecutor(name, 0);
	}

	/**
	 * The default thread factory
	 */
	static class NamedThreadFactory implements ThreadFactory {
		static final AtomicInteger poolNumber = new AtomicInteger(1);
		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger(1);
		final String namePrefix;

		NamedThreadFactory(String executorName) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null)? s.getThreadGroup() :
					Thread.currentThread().getThreadGroup();
			namePrefix = executorName+"-" +
					poolNumber.getAndIncrement() +
					"-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
}
