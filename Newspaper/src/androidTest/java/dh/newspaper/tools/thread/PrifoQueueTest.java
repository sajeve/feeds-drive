package dh.newspaper.tools.thread;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.MainActivity;
import dh.tool.thread.prifo.IPrifosable;
import dh.tool.thread.prifo.PrifoQueue;
import dh.tool.thread.prifo.PrifoTask;

/**
 * Created by hiep on 9/06/2014.
 */
public class PrifoQueueTest extends ActivityInstrumentationTestCase2<MainActivity> {

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
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return id;
		}
	}

	public PrifoQueueTest() {
		super(MainActivity.class);
	}

	public void testOfferBasic() {
		PrifoQueue queue = new PrifoQueue();
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

		assertEquals(2, queue.peek().getPriority());
		assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t4", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
	}

	PrifoQueue queue = new PrifoQueue();
	IPrifosable[] t;

	@Override
	protected void setUp() throws Exception {
		queue = new PrifoQueue();
		t = new IPrifosable[] {
				new WorkflowTask("t0"),
				new WorkflowTask("t1").setFocus(true),
				new WorkflowTask("t2"),
				new WorkflowTask("t3"),
				new WorkflowTask("t4").setFocus(true),
				new WorkflowTask("t5")
		};
	}

	public void testOfferActiveTask1() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1

		assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
	}
	public void testOfferActiveTask2() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2

		assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t3", ((PrifoTask) queue.poll()).getMissionId());
	}
	public void testOfferActiveTask3() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2
		queue.offer(t[2]); //hit t2 again -> t2.priority = 2

		assertTrue(t[1].isFocused());
		queue.offer(t[4]); //active = t4 (t1 becomes normal)
		assertFalse(t[1].isFocused());

		assertEquals("t4", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t2", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t0", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId());
		assertEquals("t3", ((PrifoTask) queue.poll()).getMissionId());
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
