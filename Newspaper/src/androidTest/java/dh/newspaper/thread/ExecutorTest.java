package dh.newspaper.thread;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.MainActivity;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoExecutorFactory;
import dh.tool.thread.prifo.PrifoQueue;
import dh.tool.thread.prifo.PrifoTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hiep on 3/06/2014.
 */
public class ExecutorTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final Logger Log = LoggerFactory.getLogger(ExecutorTest.class);

	public ExecutorTest() {
		super(MainActivity.class);
	}

	public void testQueueSize() throws InterruptedException {
		final int totalDuration = 60*1000;
		final int oneThreadDuration=200;

		PrifoExecutor executor = PrifoExecutorFactory.newPrifoExecutor("Q", 2);

		final int N = totalDuration/oneThreadDuration;
		Log.info("Call execute "+N+" times");
		for (int i = 0; i<N; i++) {
			final int x = i;

			PrifoTask t = new PrifoTask() {
				@Override
				public String getMissionId() {
					return "Integer.toString(x)";
				}

				@Override
				public void onEnterQueue(PrifoQueue queue) {
					Log.info(String.format("#%s Enter queue size = %d", getMissionId(), queue.size()));
				}

				@Override
				public void onDequeue(PrifoQueue queue) {
					Log.info(String.format("#%s DeQueue size = %d", getMissionId(), queue.size()));
				}

				@Override
				public void run() {
					try {
						Log.info(String.format("#%s run",getMissionId()));
						Thread.sleep(oneThreadDuration);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};

			executor.execute(t);
		}

		Thread.sleep(totalDuration);
	}

}