package dh.newspaper.tools;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.MainActivity;

/**
 * Created by hiep on 9/06/2014.
 */
public class BumpBlockingQueueTest extends ActivityInstrumentationTestCase2<MainActivity> {

	public static class WorkflowTask extends BumpTask {
		private String id;

		public WorkflowTask(String id) {
			this.id = id;
		}

		@Override
		public String getId() {
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
	}

	public BumpBlockingQueueTest() {
		super(MainActivity.class);
	}

	public void testOfferBasic() {
		BumpQueue queue = new BumpQueue(3);
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
		queue.offer(t[4].hit()); //t4.priority = 1

		assertEquals(2, queue.find("t1").getPriority());
		assertEquals("t1", ((BumpTask) queue.poll()).getId());
		assertEquals("t4", ((BumpTask) queue.poll()).getId());
		assertEquals("t2", ((BumpTask) queue.poll()).getId());
		assertEquals("t0", ((BumpTask) queue.poll()).getId());
	}



	BumpQueue queue = new BumpQueue(3);
	BumpTask[] t;

	@Override
	protected void setUp() throws Exception {
		queue = new BumpQueue(3);
		t = new BumpTask[] {
				new WorkflowTask("t0"),
				new WorkflowTask("t1").setActive(true),
				new WorkflowTask("t2"),
				new WorkflowTask("t3"),
				new WorkflowTask("t4").setActive(true),
				new WorkflowTask("t5")
		};
	}

	public void testOfferActiveTask1() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1

		assertEquals("t1", ((BumpTask) queue.poll()).getId());
		assertEquals("t0", ((BumpTask) queue.poll()).getId());
	}
	public void testOfferActiveTask2() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2

		assertEquals("t1", ((BumpTask) queue.poll()).getId());
		assertEquals("t2", ((BumpTask) queue.poll()).getId());
		assertEquals("t3", ((BumpTask) queue.poll()).getId());
		assertEquals("t0", ((BumpTask) queue.poll()).getId());
	}
	public void testOfferActiveTask3() {
		queue.offer(t[0]);
		queue.offer(t[1]); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2
		queue.offer(t[2]); //hit t2 again -> t2.priority = 2

		assertTrue(t[1].isActive());
		queue.offer(t[4]); //active = t4 (t1 becomes normal)
		assertFalse(t[1].isActive());

		assertEquals("t4", ((BumpTask) queue.poll()).getId());
		assertEquals("t2", ((BumpTask) queue.poll()).getId());
		assertEquals("t3", ((BumpTask) queue.poll()).getId());
		assertEquals("t1", ((BumpTask) queue.poll()).getId());
		assertEquals("t0", ((BumpTask) queue.poll()).getId());
	}

//	private void testPriorityQueueOffer() {
//		PriorityQueue<BumpTask> queue = new PriorityQueue<>(3);
//
//		BumpTask[] t = new BumpTask[] {
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
//		assertEquals("t1", ((BumpTask) queue.poll()).getId());
//	}
}
