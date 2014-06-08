package dh.newspaper.workflow;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.PathToContent;
import dh.newspaper.parser.ContentParser;
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
		SelectTagWorkflow stw = new SelectTagWorkflow(this.getActivity(), "world", null, null, null, null);
		stw.run();
		assertTrue(stw.getResultSize() > 0);
	}

}