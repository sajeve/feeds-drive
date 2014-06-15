package dh.newspaper.tools.thread;

/**
 * A task must implement this interface to use in {@link dh.newspaper.tools.thread.PrifoBlockingQueue}
 * Created by hiep on 12/06/2014.
 */
public interface IPrifosable extends Runnable, ICancellation {
	public int getPriority();
	public IPrifosable increasePriority();
	public IPrifosable setFocus(boolean focused);
	public boolean isFocused();
	public void onEnterQueue(PrifoQueue queue);
	public void onDequeue(PrifoQueue queue);
}
