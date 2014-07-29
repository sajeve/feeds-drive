package dh.tool.thread.prifo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link dh.tool.thread.prifo.PrifoTask} which can only be executed one.
 * Created by hiep on 12/06/2014.
 */
public abstract class OncePrifoTask extends PrifoTask {
	private static final Logger log = LoggerFactory.getLogger(OncePrifoTask.class);
	private volatile boolean used = false;
	private volatile boolean running = true;
	private final ReentrantLock lock = new ReentrantLock();

	@Override
	public void run() {
		try {
			if (isCancelled()) {
				return;
			}
			final ReentrantLock lock = this.lock;
			lock.lock();
			running = true;
			if (used) {
				throw new IllegalStateException("OncePrifoTask run twice");
			}
			used = true;
			try {
				perform();
			} finally {
				running = false;
				lock.unlock();
			}
		}
		catch (Exception ex) {
			log.error("Un-catch error", ex);
		}
	}

	public boolean isUsed() {
		return used;
	}

	public boolean isRunning() {
		return running;
	}

	abstract public void perform();
}
