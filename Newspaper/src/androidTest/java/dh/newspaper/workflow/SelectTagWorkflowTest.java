package dh.newspaper.workflow;

import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.google.common.base.Stopwatch;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.services.BackgroundTasksManager;
import dh.tool.thread.prifo.IQueueEmptyCallback;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoExecutorFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 3/06/2014.
 */
public class SelectTagWorkflowTest extends ActivityInstrumentationTestCase2<MainActivity> {
	private static final String TAG = SelectTagWorkflowTest.class.getName();

	public SelectTagWorkflowTest() {
		super(MainActivity.class);
	}

//	ContentParser mContentParser;
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//		mContentParser = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);;
//		//((MyApplication)this.getActivity().getApplication()).getObjectGraph().inject(this);
//	}

	public void testEnv() {
		System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
		Log.i(TAG, Environment.getExternalStorageDirectory().getAbsolutePath());
	}


	public void testSelectTagWorkflow1() {
		SelectTagWorkflow stw = new SelectTagWorkflow(this.getActivity(), "technology", null, null, true, Constants.ARTICLES_PER_PAGE, null, null);
		stw.run();
		assertTrue(stw.getTotalSize() > 0);
	}

	public void testSelectTagWorkflow2() throws InterruptedException {
		runSelectTagWorkflow(8);
	}

	private Object completion = new Object();

	private void runSelectTagWorkflow(int numberOfThread) throws InterruptedException {

		PrifoExecutor articlesLoader = PrifoExecutorFactory.newPrifoExecutor(1, 2);
		articlesLoader.setCorePoolSize(numberOfThread);
		articlesLoader.setMaximumPoolSize(numberOfThread*2);

		SelectTagWorkflow stw = new SelectTagWorkflow(this.getActivity(), "technology",
				null, null, true,
				Constants.ARTICLES_PER_PAGE, articlesLoader, null);

		final Stopwatch sw = Stopwatch.createStarted();

		stw.run();

		articlesLoader.setQueueEmptyCallback(new IQueueEmptyCallback() {
			@Override
			public void onQueueEmpty() {
				synchronized (completion) {
					completion.notifyAll();
				}
				Log.i(TAG, "Queue empty after: "+sw.elapsed(TimeUnit.SECONDS)+ " s");
			}
		});

		synchronized (completion) {
			completion.wait();
		}

		Log.i(TAG, numberOfThread+" threads complete after: "+sw.elapsed(TimeUnit.SECONDS)+ " s");
		assertTrue(stw.getTotalSize() > 0);
	}

}