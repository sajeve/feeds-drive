package dh.tool.thread.prifo;

import dh.tool.common.StrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Hold a set of unique {@link IPrifosable}, when we add a task e in this queue,
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
	private static final Logger log = LoggerFactory.getLogger(PrifoQueue.class);
	private transient TreeSet<E> queue;
	private IQueueEmptyCallback queueEmptyCallback;
	private final String name;

	public PrifoQueue(String name, Comparator<? super E> comparator, IQueueEmptyCallback queueEmptyCallback) {
		this.queue = new TreeSet<E>(comparator);
		this.queueEmptyCallback = queueEmptyCallback;
		this.name=name;
	}
	public PrifoQueue(String name, IQueueEmptyCallback queueEmptyCallback) {
		this(name, null, queueEmptyCallback);
	}
	public PrifoQueue(String name, Comparator<? super E> comparator) {
		this(name, comparator, null);
	}
	public PrifoQueue(String name) {
		this(name, null, null);
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
			return false; //task is cancelled, no need to add it to the queue
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
		try {
			e.onEnterQueue(this);
		}
		catch (Exception ex) {
			log.warn("onEnterQueue ", ex);
		}
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
			if (((Object)resu).equals(e) || StrUtils.equalsString(resu.getMissionId(), e.getMissionId())) {
				return resu;
			}
		}
		return null;
	}

	@Override
	public E poll() {
		E e = queue.pollFirst();
		if (e!=null) {
			try {
				e.onDequeue(this);
			}
			catch (Exception ex) {
				log.warn("onDequeue ", ex);
			}
		}
		if (queueEmptyCallback!=null && queue.isEmpty()) {
			try {
				queueEmptyCallback.onQueueEmpty(getName());
			}
			catch (Exception ex) {
				log.warn("onQueueEmpty ", ex);
			}
		}
		return e;
	}

	@Override
	public E peek() {
		return queue.first();
	}

	public int countActiveTasks() {
		int c = 0;
		for (IPrifosable e : this) {
			if (!e.isCancelled()) {
				c++;
			}
		}
		return c;
	}

	public String printActiveTask() {
		StringBuilder s = new StringBuilder();
		for (IPrifosable e : this) {
			if (!e.isCancelled()) {
				s.append("{"+e.toString() + " / " + e.getPriority()+ (e.isFocused() ?" / focused": "")+"}, ");
			}
		}
		return s.toString();
	}

	public IQueueEmptyCallback getQueueEmptyCallback() {
		return queueEmptyCallback;
	}

	public void setQueueEmptyCallback(IQueueEmptyCallback queueEmptyCallback) {
		this.queueEmptyCallback = queueEmptyCallback;
	}

	public String getName() {
		return name;
	}
}
