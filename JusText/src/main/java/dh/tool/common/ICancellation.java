package dh.tool.common;

/**
 * A state provider which provide state in the process, to tell if the process is cancelled
 *
 * Created by hiep on 14/06/2014.
 */
public interface ICancellation {
	public boolean isCancelled();
}
