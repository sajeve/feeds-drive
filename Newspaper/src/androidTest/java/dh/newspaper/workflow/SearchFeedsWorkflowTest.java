package dh.newspaper.workflow;

import android.test.ActivityInstrumentationTestCase2;
import dh.newspaper.MainActivity;
import dh.newspaper.parser.FeedParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by hiep on 3/06/2014.
 */
public class SearchFeedsWorkflowTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private static final Logger Log = LoggerFactory.getLogger(SearchFeedsWorkflowTest.class);

	public SearchFeedsWorkflowTest() {
		super(MainActivity.class);
	}

	/*ContentParser mContentParser;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentParser = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);;
		//((MyApplication)this.getActivity().getApplication()).getObjectGraph().inject(this);
	}*/

	public void testSearchFeedsTask1() throws IOException, FeedParserException {
		Log.info("Start test");
		//SearchFeedsTask sft = new SearchFeedsTask(this.getActivity(), "vnexpress.net");
		//SearchFeedsTask sft = new SearchFeedsTask(this.getActivity(), "http://vnexpress.net/rss/thoi-su.rss");
		//SearchFeedsTask sft = new SearchFeedsTask(this.getActivity(), "http://vnexpress.net/rss");
		SearchFeedsWorkflow sft = new SearchFeedsWorkflow(this.getActivity(), "http://dantri.com.vn/rss/");
		sft.run();
	}



}