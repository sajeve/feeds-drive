package dh.tool.thread;

import java.util.concurrent.CancellationException;

/**
 * Created by hiep on 31/08/2014.
 */
public class ThreadUtils {
	public static void checkCancellation(ICancellation cancellation) {
		if (cancellation != null && cancellation.isCancelled()) {
			throw new CancellationException();
		}
	}
	public static void checkCancellation(ICancellation cancellation, String message) {
		if (cancellation != null && cancellation.isCancelled()) {
			throw new CancellationException(message);
		}
	}
}
