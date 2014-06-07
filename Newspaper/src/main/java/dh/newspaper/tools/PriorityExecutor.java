package dh.newspaper.tools;

import java.util.concurrent.*;

/**
 * Created by hiep on 5/06/2014.
 */
public class PriorityExecutor extends ThreadPoolExecutor {
	public static final int LOW = 10;
	public static final int MEDIUM = 20;
	public static final int HIGH = 30;

	public PriorityExecutor(int corePoolSize, int maximumPoolSize,
							long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}
	//Utitlity method to create thread pool easily
	public static PriorityExecutor newFixedThreadPool(int nThreads) {
		return new PriorityExecutor(nThreads, nThreads, 0L,
				TimeUnit.MILLISECONDS, new PriorityBlockingQueue<Runnable>());
	}

	public static PriorityExecutor newCachedThreadPool(int maxThread) {
		return new PriorityExecutor(0, maxThread,
				60L, TimeUnit.SECONDS,
				new PriorityBlockingQueue<Runnable>());
	}

	//Submit with New comparable task
	public Future<?> submit(Runnable task, int priority) {
		return super.submit(new ComparableFutureTask(task, null, priority));
	}

	//execute with New comparable task
	public void execute(Runnable command, int priority) {
		super.execute(new ComparableFutureTask(command, null, priority));
	}
}
