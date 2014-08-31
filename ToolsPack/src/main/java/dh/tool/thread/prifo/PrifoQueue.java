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

	/**
	 * tasks are queued and ordered by this comparator.
	 * - The focused task is the top-most priority
	 * - Priority attribute
	 * - The natural order of item (if items are comparable)
	 * - The missionId
	 *
	 * them item with the same missionId are always equals even if the natural order are not the same
	 */
	public final Comparator PrifoComparator = new Comparator<E>() {
		@Override
		public int compare(E o1, E o2) {
			if (o1==null && o2==null) {
				return 0;
			}
			else {
				if (o1 == null) {
					return 1000;
				}
				if (o2 == null) {
					return -1000;
				}
			}

			if (StrUtils.equalsString(o1.getMissionId(), o2.getMissionId())) {
				return 0;
			}

			if (o1.isFocused() && !o2.isFocused()) {
				return Integer.MIN_VALUE;
			}
			if (!o1.isFocused() && o2.isFocused()) {
				return Integer.MAX_VALUE;
			}

			int c = o2.getPriority()-o1.getPriority();
			if (c!=0) {
				return c;
			}

			if (o1 instanceof Comparable) {
				c = ((Comparable) o1).compareTo(o2);
				if (c!=0) {
					return c;
				}
			}

			if (o2.getMissionId()==null) {
				return -10;
			}
			if (o1.getMissionId()==null) {
				return 10;
			}
			return o1.getMissionId().compareTo(o2.getMissionId());
		}
	};

	public PrifoQueue(String name, IQueueEmptyCallback queueEmptyCallback) {
		this.queue = new TreeSet<E>(PrifoComparator);
		this.queueEmptyCallback = queueEmptyCallback;
		this.name=name;
	}
	public PrifoQueue(String name) {
		this(name, null);
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
		}

		E existed = findItem(e);

		if (existed != null) {
			queue.remove(existed);
			e.setPriority(existed.isCancelled() ? 0 : existed.getPriority()+1);
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
