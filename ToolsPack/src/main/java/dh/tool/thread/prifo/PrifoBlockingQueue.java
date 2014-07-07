package dh.tool.thread.prifo;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unbounded blocking queue
 * Created by hiep on 11/06/2014.
 */
public class PrifoBlockingQueue<E extends IPrifosable> extends AbstractQueue<E> implements BlockingQueue<E> {

	private final PrifoQueue<E> queue = new PrifoQueue<E>();

	/**
	 * Lock used for all public operations
	 */
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	@Override
	public Iterator<E> iterator() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return Arrays.asList((E[])queue.toArray()).iterator();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return queue.size();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean offer(E e) {
		if (e == null)
			throw new NullPointerException();
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			queue.offer(e);
			notEmpty.signal();
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public void put(E e) throws InterruptedException {
		offer(e);
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		return offer(e); // never need to block
	}

	@Override
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ( (result = queue.poll()) == null)
				notEmpty.await();
		} finally {
			lock.unlock();
		}
		return result;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		E result;
		try {
			while ( (result = queue.poll()) == null && nanos > 0)
				nanos = notEmpty.awaitNanos(nanos);
		} finally {
			lock.unlock();
		}
		return result;
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int drainTo(Collection<? super E> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	public int drainTo(Collection<? super E> c, int maxElements) {
		if (c == null)
			throw new NullPointerException();
		if (c == this)
			throw new IllegalArgumentException();
		if (maxElements <= 0)
			return 0;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			int n = Math.min(queue.size(), maxElements);
			for (int i = 0; i < n; i++) {
				c.add((E) queue.poll()); // In this order, in case add() throws.
			}
			return n;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return queue.poll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return queue.peek();
		} finally {
			lock.unlock();
		}
	}
}
