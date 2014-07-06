package dh.newspaper.workflow;

import android.test.ActivityInstrumentationTestCase2;
import com.google.common.base.Strings;
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
		Feeds feeds = mContentParser.parseFeeds("http://www.huffingtonpost.com/tag/asian-americans/feed", "utf-8", null);
		assertTrue(feeds.size() > 0);
		FeedItem feedItem = feeds.get(0);

		assertEquals("http://www.huffingtonpost.com/kiran-ahuja/reflecting-on-50-years-of_b_5553462.html", feedItem.getUri());
		SelectArticleWorkflow saw = new SelectArticleWorkflow(this.getActivity(), feedItem, Constants.ARTICLE_TTL, true, null);
		saw.run();
		assertTrue(!Strings.isNullOrEmpty(feedItem.getTextPlainDescription()));
	}

}