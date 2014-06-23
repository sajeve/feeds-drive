package dh.newspaper.tools.thread;

import java.util.*;

/**
 * Hold a set of unique {@link dh.newspaper.tools.thread.IPrifosable}, when we add a task e in this queue,
 * - if the e is already in the queue, so its priority will increase
 * - if the e is focused, it will move to the top of the queue
 *
 * The queue hold only 1 focused task a time. The later added focused task disables the old one (it become normal task)
 *
 * See also {@link #offer(IPrifosable)}
 *
 * Created by hiep on 11/06/2014.
 */
public class PrifoQueue<E extends IPrifosable> extends AbstractQueue<E> {
	private transient TreeSet<E> queue;

	public PrifoQueue(Comparator<? super E> comparator) {
		this.queue = new TreeSet<E>(comparator);
	}
	public PrifoQueue() {
		this(null);
	}

	@Override
	public Iterator<E> iterator() {
		return queue.iterator();
	}

	@Override
	public int size() {
		return queue.size();
	}

	/**
	 * add a task e to the queue.
	 * <ul>
	 * <li>if the task e is cancelled, do nothing</li>
	 * <li>if the a twin e' of e is already in the queue:
	 * 		<ul>
	 * 	  	<li>increase the priority of e'</li>
	 *     	<li>if the twin e' in the queue is cancelled, replace it by e (its priority will also reset with e)</li>
	 * 		</ul>
	 * </li>
	 * <li>if e is focused, so that e' will take over the focus (there is at most 1 focused task at the top of the queue)</li>
	 * </ul>
	 */
	@Override
	public boolean offer(E e) {
		if (e.isCancelled()) {
			return true; //task is cancelled, no need to add it to the queue
		}
		E existed = findItem(e);
		if (e.isFocused()) {
			/*
			 * update focusing so that queue can only has 1 focused item
			 */
			E lastFocusedItem = findFocusedItem();
			if (lastFocusedItem != null) {
				queue.remove(lastFocusedItem);
				lastFocusedItem.setFocus(false);
				queue.add(lastFocusedItem);
			}
			if (existed!=null) {
				existed.setFocus(true);
				queue.remove(existed);
				if (existed.isCancelled()) {
					/*
					replace the existed task which was cancelled by the fresh new twin
					the priority will be reset with the priority of the twin
					 */
					existed = e;
				}
				else {
					existed.increasePriority();
				}
				queue.add(existed);
				return true;
			}
		}
		else {
			if (existed!=null) {
				queue.remove(existed);
				if (existed.isCancelled()) {
					/*
					replace the existed task which was cancelled by the fresh new twin
					the priority will be reset with the priority of the twin
					 */
					existed = e;
				}
				else {
					existed.increasePriority();
				}
				queue.add(existed);
				return true;
			}
		}
		queue.add(e);
		e.onEnterQueue(this);
		return true;
	}

	public E findFocusedItem() {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			E resu = it.next();
			if (resu.isFocused()) {
				return resu;
			}
		}
		return null;
	}
	public E findItem(E e) {
		Iterator<E> it = iterator();
		while (it.hasNext()) {
			E resu = it.next();
			if (((Object)resu).equals(e) || (queue.comparator()!=null && queue.comparator().compare(resu,e) == 0)) {
				return resu;
			}
		}
		return null;
	}

	@Override
	public E poll() {
		E e = queue.pollFirst();
		if (e!=null)
			e.onDequeue(this);
		return e;
	}

	@Override
	public E peek() {
		return queue.first();
	}
}
