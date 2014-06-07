package dh.newspaper.tools;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hiep on 7/06/2014.
 */
/**
 * The default thread factory
 */
public class PriorityThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	private final int priority;

	public PriorityThreadFactory(int priority) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() :
				Thread.currentThread().getThreadGroup();
		namePrefix = "pool-" +
				poolNumber.getAndIncrement() +
				"-thread-";
		this.priority = priority;
	}

	public PriorityThreadFactory() {
		this(Thread.NORM_PRIORITY);
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r,
				namePrefix + threadNumber.getAndIncrement(),
				0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != priority)
			t.setPriority(priority);
		return t;
	}
}
