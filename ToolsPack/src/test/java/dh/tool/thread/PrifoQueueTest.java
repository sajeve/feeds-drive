package dh.tool.thread;

import dh.tool.common.DateUtils;
import dh.tool.thread.prifo.*;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by hiep on 9/06/2014.
 */
public class PrifoQueueTest {
	private static final Logger Log = LoggerFactory.getLogger(PrifoQueueTest.class);

	public static class WorkflowTask extends PrifoTask implements Comparable {
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

		private Date publishedDate;
		public Date getPublishedDate() {
			return publishedDate;
		}
		public WorkflowTask setPublishedDate(Date publishedDate) {
			this.publishedDate = publishedDate;
			return this;
		}
		public int compareTo(Object another) {
			//return thisPublishDate - anotherPublishDate
			Date anotherPublishedDate = ((WorkflowTask)another).getPublishedDate();
			Date publishedDate = getPublishedDate();
			if (publishedDate==null && anotherPublishedDate==null) {
				return 0;
			}
			if (anotherPublishedDate == null) {
				return -1;
			}
			if (publishedDate == null) {
				return 1;
			}
			return anotherPublishedDate.compareTo(publishedDate);
		}
	}

	@Test
	public void testCompareTo() {
		Assert.assertTrue(DateUtils.createDate(2014,12,25).compareTo(DateUtils.createDate(2014,12,26))<0);
		Assert.assertTrue("a".compareTo("b") < 0);
	}

	@Test
	public void testOfferBasic() {
		PrifoQueue queue = new PrifoQueue("testOfferBasic");
		WorkflowTask[] t = new WorkflowTask[] {
				new WorkflowTask("t0"),
				new WorkflowTask("t1"),
				new WorkflowTask("t2"),
				new WorkflowTask("t3"),
				new WorkflowTask("t4"),
				new WorkflowTask("t5").setPublishedDate(DateUtils.createDate(2014, 9, 30)),
				new WorkflowTask("t6").setPublishedDate(DateUtils.createDate(2014, 8, 29)),
				new WorkflowTask("t7"),
				new WorkflowTask("t8").setPublishedDate(DateUtils.createDate(2014, 9, 15))
		};

		queue.offer(t[0]);
		queue.offer(t[6]);
		queue.offer(t[1]);
		queue.offer(t[2].setFocus(true));
		queue.offer(t[1]); //hit t1
		queue.offer(t[1]); //hit t1 again -> t1.priority = 2
		queue.offer(t[4].setPriority(1)); //t4.priority = 1
		queue.offer(t[5]);
		queue.offer(t[7].setFocus(true));
		queue.offer(t[8]);



		//Assert.assertTrue(queue.PrifoComparator.compare(t[1], t[4]) < 0);

		//t0 < t6 < t5
		//Assert.assertTrue(queue.PrifoComparator.compare(t[5], t[0]) < 0);
		/*Assert.assertTrue("a".compareTo("b") < 0);
		Assert.assertTrue(t[6].compareTo(t[5])>0);
		Assert.assertTrue(t[0].compareTo(t[6])>0);*/


		Assert.assertEquals("t7", ((PrifoTask) queue.poll()).getMissionId()); //focused
		Assert.assertEquals("t1", ((PrifoTask) queue.poll()).getMissionId()); //t1.priority = 2
		Assert.assertEquals("t4", ((PrifoTask) queue.poll()).getMissionId()); //t4.priority = 1
		Assert.assertEquals("t5", ((PrifoTask) queue.poll()).getMissionId()); //t5.publishedDate = 2014-9-30
		Assert.assertEquals("t8", ((PrifoTask) queue.poll()).getMissionId()); //t8.publishedDate = 2014-9-15
		Assert.assertEquals("t6", ((PrifoTask) queue.poll()).getMissionId()); //t6.publishedDate = 2014-8-29
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
		PrifoQueue queue = new PrifoQueue("queue");

		queue.offer(t[0]);
		queue.offer(t[1].setFocus(true)); //active = t1
		queue.offer(t[2]);
		queue.offer(t[3]);
		queue.offer(t[2]); //hit t2

		//1 2 0 3

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

		PrifoExecutor executor = PrifoExecutorFactory.newPrifoExecutor("testQueueSize", 2);

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
