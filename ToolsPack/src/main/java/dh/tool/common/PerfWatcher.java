package dh.tool.common;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import org.slf4j.Logger;

import java.security.InvalidParameterException;
import java.util.concurrent.TimeUnit;

/**
 * A logger integrate a Stopwatch, which start on creation
 * - All the short methods (t, d, i..) will reset the stopwatch
 * - dg and ig print the global time elapses from the creation
 * - if weakId = true: only display id if it is not contains in the message
 * Created by hiep on 30/06/2014.
 */
public class PerfWatcher {
	private final Logger log;
	private final String id;
	private final Stopwatch globalStopwatch;
	private final Stopwatch stopwatch;
	private final boolean weakId;

	public PerfWatcher(Logger log, String id, boolean weakIdDisplay) {
		if (log == null || Strings.isNullOrEmpty(id)) {
			throw new InvalidParameterException("log and id must not null");
		}
		this.log = log;
		this.stopwatch = Stopwatch.createStarted();
		this.globalStopwatch = Stopwatch.createStarted();
		this.id = id;
		this.weakId = weakIdDisplay;
	}

	public PerfWatcher(Logger log, String id) {
		this(log, id, false);
	}

	/**
	 * Add duration, and Id
	 */
	private String messageWithTime(String message) {
		if (weakId) {
			if (!Strings.isNullOrEmpty(message) && message.contains(id)) {
				//no need to display Id
				return String.format("%6d ms  %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message);
			}
		}
		return String.format("%6d ms  %s - %s", stopwatch.elapsed(TimeUnit.MILLISECONDS), message, id);
	}
	private String globalMessageWithTime(String message) {
		if (weakId) {
			if (!Strings.isNullOrEmpty(message) && message.contains(id)) {
				//no need to display Id
				return String.format("Total %6d ms  %s", globalStopwatch.elapsed(TimeUnit.MILLISECONDS), message);
			}
		}
		return String.format("Total %6d ms  %s - %s", globalStopwatch.elapsed(TimeUnit.MILLISECONDS), message, id);
	}
	private String simpleMessage(String message) {
		if (weakId) {
			if (!Strings.isNullOrEmpty(message) && message.contains(id)) {
				//no need to display Id
				return message;
			}
		}
		return message + " - " + id;
	}

	public void t(String message) {
		log.trace(messageWithTime(message));
		resetStopwatch();
	}

	public void d(String message) {
		log.debug(messageWithTime(message));
		resetStopwatch();
	}
	public void d(String message, Throwable e) {
		log.debug(messageWithTime(message), e);
		resetStopwatch();
	}
	public void i(String message) {
		log.info(messageWithTime(message));
		resetStopwatch();
	}
	public void i(String message, Throwable e) {
		log.info(messageWithTime(message), e);
		resetStopwatch();
	}
	public void w(String message) {
		log.warn(messageWithTime(message));
		resetStopwatch();
	}
	public void w(String message, Throwable e) {
		log.warn(messageWithTime(message), e);
		resetStopwatch();
	}
	public void e(String message) {
		log.error(messageWithTime(message));
		resetStopwatch();
	}
	public void e(String message, Throwable e) {
		log.error(messageWithTime(message), e);
		resetStopwatch();
	}

	public void trace(String message) {
		log.trace(simpleMessage(message));
	}
	public void debug(String message) {
		log.debug(simpleMessage(message));
	}
	public void info(String message) {
		log.info(simpleMessage(message));
	}
	public void warn(String message) {
		log.warn(simpleMessage(message));
	}
	public void warn(String message, Throwable e) {
		log.warn(message, e);
	}
	public void error(String message) {
		log.error(simpleMessage(message));
	}
	public void error(String message, Throwable e) {
		log.error(message, e);
	}

	public void dg(String message) {
		log.debug(globalMessageWithTime(message));
	}
	public void ig(String message) {
		log.info(globalMessageWithTime(message));
	}
	public void wg(String message) {
		log.warn(globalMessageWithTime(message));
	}


	public void resetStopwatch() {
		stopwatch.reset().start();
	}
	public void resetGlobalStopwatch() {
		globalStopwatch.reset().start();
		stopwatch.reset().start();
	}
}
