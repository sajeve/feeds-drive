package dh.tool.common;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * A logger integrate a Stopwatch, which start on creation
 * - All the short methods (t, d, i..) will reset the stopwatch
 * - dg and ig print the global time elapses from the creation
 * Created by hiep on 30/06/2014.
 */
public class PerfWatcher {
	private final Logger log;
	private final String id;
	private final Stopwatch globalStopwatch;
	private final Stopwatch stopwatch;

	public PerfWatcher(Logger log, String id) {
		this.log = log;
		this.stopwatch = Stopwatch.createStarted();
		this.globalStopwatch = Stopwatch.createStarted();
		this.id = id;
	}

	public void t(String message) {
		log.trace(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
		resetStopwatch();
	}
	public void d(String message) {
		log.debug(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
		resetStopwatch();
	}
	public void i(String message) {
		log.info(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
		resetStopwatch();
	}
	public void w(String message) {
		log.warn(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
		resetStopwatch();
	}
	public void w(String message, Throwable e) {
		log.warn(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message), e);
		resetStopwatch();
	}
	public void e(String message) {
		log.error(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
		resetStopwatch();
	}
	public void e(String message, Throwable e) {
		log.error(String.format("%5d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message), e);
		resetStopwatch();
	}

	public void trace(String message) {
		log.trace(message + " - " + id);
	}
	public void debug(String message) {
		log.debug(message + " - " + id);
	}
	public void info(String message) {
		log.info(message + " - " + id);
	}
	public void warn(String message) {
		log.warn(message + " - " + id);
	}
	public void warn(String message, Throwable e) {
		log.warn(message, e);
	}
	public void error(String message) {
		log.error(message + " - " + id);
	}
	public void error(String message, Throwable e) {
		log.error(message, e);
	}

	public void dg(String message) {
		log.debug(String.format("Total %5d ms  %s - %s", globalStopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
	}
	public void ig(String message) {
		log.info(String.format("Total %5d ms  %s - %s", globalStopwatch.elapsed(TimeUnit.MILLISECONDS), message, id));
	}

	public void resetStopwatch() {
		stopwatch.reset().start();
	}
	public void resetGlobalStopwatch() {
		globalStopwatch.reset().start();
	}
}
