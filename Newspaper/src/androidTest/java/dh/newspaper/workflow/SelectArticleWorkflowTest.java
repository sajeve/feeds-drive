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
import dh.tool.common.ICancellation;

import java.io.IOException;

/**
 * Created by hiep on 3/06/2014.
 */
public class SelectArticleWorkflowTest extends ActivityInstrumentationTestCase2<MainActivity> {

	public SelectArticleWorkflowTest() {
		super(MainActivity.class);
	}

	ContentParser mContentParser;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mContentParser = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);;
		//((MyApplication)this.getActivity().getApplication()).getObjectGraph().inject(this);
	}

	public void testSelectArticleWorkflow1() throws IOException, FeedParserException {
		Feeds feeds = mContentParser.parseFeeds("http://vnexpress.net/rss/thoi-su.rss", "utf-8", new ICancellation() {
			@Override
			public boolean isCancelled() {
				return false;
			}
		});
		assertTrue(feeds.size() > 0);
		FeedItem feedItem = feeds.get(0);

		SelectArticleWorkflow saw = new SelectArticleWorkflow(this.getActivity(), feedItem, Constants.ARTICLE_TTL, true, null);

		PathToContent pathToContent = saw.findFirstMatchingPathToContent("http://vnexpress.net/tin-tuc/thoi-su/ha-noi-don-dep-day-dien-cap-chang-chit-tren-pho-2998401.html");
		assertNotNull(pathToContent);
		assertEquals("vn", pathToContent.getLanguage());

		saw.run();
	}

}