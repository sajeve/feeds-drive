package dh.tool.thread.prifo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 12/06/2014.
 */
public class PrifoExecutors {
	public static ExecutorService newCachedThreadExecutor(int corePoolSize, int maxPoolSize) {
		return new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new PrifoBlockingQueue());
	}
}
