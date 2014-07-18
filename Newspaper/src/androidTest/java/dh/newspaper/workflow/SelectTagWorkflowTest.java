package dh.newspaper.workflow;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.parser.FeedParserException;

import java.io.IOException;

/**
 * Created by hiep on 3/06/2014.
 */
public class SelectTagWorkflowTest extends ActivityInstrumentationTestCase2<MainActivity> {

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

	public void testSelectTagWorkflow1() throws IOException, FeedParserException {
		SelectTagWorkflow stw = new SelectTagWorkflow(this.getActivity(), "world", null, null, true, Constants.ARTICLES_PER_PAGE, null, null);
		stw.run();
		assertTrue(stw.getTotalSize() > 0);
	}

}