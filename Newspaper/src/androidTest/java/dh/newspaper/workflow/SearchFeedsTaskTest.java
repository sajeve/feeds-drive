package dh.newspaper.workflow;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.MyApplication;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.DaoSession;
import dh.newspaper.modules.AppContextModule;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.FeedParserException;
import dh.newspaper.services.BackgroundTasksManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by hiep on 3/06/2014.
 */
public class SearchFeedsTaskTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private static final Logger Log = LoggerFactory.getLogger(SearchFeedsTaskTest.class);

	public SearchFeedsTaskTest() {
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
		SearchFeedsTask sft = new SearchFeedsTask(this.getActivity(), "http://dantri.com.vn/rss/");
		sft.run();
	}



}