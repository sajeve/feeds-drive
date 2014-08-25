package dh.tool.thread;

import dh.tool.thread.prifo.*;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hiep on 9/06/2014.
 */
public class PrifoQueueTest {
	private static final Logger Log = LoggerFactory.getLogger(PrifoQueueTest.class);


	public static class WorkflowTask extends PrifoTask {
		private String id;

		public WorkflowTask(String id) {
			this.id = id;
		}

		@Override
		public String getMissionId() {
			return id;
		}

		@Override
		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return id;
		}

		@Override
		public void onEnterQueue(PrifoQueue queue) {

		}

		@Override
		public void onDequeue(PrifoQueue queue) {

		}

		@Override
		public int compareTo(Object another) {
			int c = super.compareTo(another);
			if (c==0) {
				return this.getMissionId().compareTo(((PrifoTask)another).getMissionId());
			}
			else {
				return c;
			}
		}
	}

	@Test
	public void testOfferBasic() {
		PrifoBlockingQueue queue = new PrifoBlockingQueue("testOfferBasic");
		WorkflowTask[] t = new WorkflowTask[] {
				new WorkflowTask("t0"),
				new WorkflowTask("t1"),
				new WorkflowTask("t2"),
				new WorkflowTask("t3"),
				new WorkflowTask("t4")
		};

		queue.offer(t[0]);
		queue.offer(t[1]);
		queue.offer(t[2]);
		queue.offer(t[1]); //hit t1
		queue.offer(t[1]); //hit t1 again -> t1.priority = 2
		queue.offer(t[4].increasePriority()); //t4.priority = 1

		Assert.assertEquals(2, queue.peek().getPriority());
		Assert.assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t4", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
	}

	PrifoBlockingQueue queue = new PrifoBlockingQueue("queue");
	IPrifosable[] t;

	@Before
	public void setUp() throws Exception {
		queue = new PrifoBlockingQueue("setUp");
		t = new IPrifosable[] {
				new WorkflowTask("t0"),
				new WorkflowTask("t1").setFocus(true),
				new WorkflowTask("t2"),
				new WorkflowTask("t3"),
				new WorkflowTask("t4").setFocus(true),
				new WorkflowTask("t5")
		};
	}

	@Test
	public void testOfferActiveTask1() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1

		Assert.assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
	}

	@Test
	public void testOfferActiveTask2() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2

		Assert.assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t3", ((PrifoTask) queue.poll()).getMissionId());
	}

	@Test
	public void testOfferActiveTask3() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2
		queue.offer(t[2]); //hit t2 again -> t2.priority = 2

		Assert.assertTrue(t[1].isFocused());
		queue.offer(t[4]); //active = t4 (t1 becomes normal)
		Assert.assertFalse(t[1].isFocused());

		Assert.assertEquals("t4", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		Assert.assertEquals("t3", ((PrifoTask) queue.poll()).getMissionId());
	}

	//@Test
	public void testQueueSize() throws InterruptedException {
		final int totalDuration = 60*1000;
		final int oneThreadDuration=200;

		PrifoExecutor executor = PrifoExecutorFactory.newPrifoExecutor("testQueueSize", 2, Integer.MAX_VALUE);

		final int N = totalDuration/oneThreadDuration;
		Log.info("Call execute "+N+" times");
		for (int i = 0; i<N; i++) {
			final int x = i;

			PrifoTask t = new PrifoTask() {
				@Override
				public String getMissionId() {
					return Integer.toString(x);
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

//	private void testPriorityQueueOffer() {
//		PriorityQueue<PrifoTask> queue = new PriorityQueue<>(3);
//
//		PrifoTask[] t = new PrifoTask[] {
//				new WorkflowTask("t0"),
//				new WorkflowTask("t1").hit(),
//				new WorkflowTask("t2")
//		};
//
//		assertTrue(t[1].compareTo(t[0]) < 0);
//
//		queue.offer(t[0]);
//		queue.offer(t[1]);
//		queue.offer(t[2]);
//
//		assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
//	}
}
