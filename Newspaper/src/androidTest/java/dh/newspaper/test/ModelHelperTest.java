//package dh.newspaper.test;
//
//import android.test.ActivityInstrumentationTestCase2;
//import dh.newspaper.MainActivity;
//import dh.newspaper.MyApplication;
//import dh.newspaper.cache.ModelHelper;
//import dh.newspaper.model.FeedItem;
//import dh.newspaper.model.Feeds;
//import dh.newspaper.model.generated.Article;
//import dh.newspaper.model.generated.PathToContent;
//import dh.newspaper.model.generated.Subscription;
//import dh.newspaper.parser.ContentParser;
//import dh.newspaper.parser.FeedParserException;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.TreeSet;
//
///**
// * Created by hiep on 10/05/2014.
// */
//public class ModelHelperTest extends ActivityInstrumentationTestCase2<MainActivity> {
//
//	public ModelHelperTest() {
//		super(MainActivity.class);
//	}
//
//	ModelHelper mModelHelper;
//	ContentParser mContentParser;
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//		mModelHelper = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ModelHelper.class);
//		mContentParser = ((MyApplication)this.getActivity().getApplication()).getObjectGraph().get(ContentParser.class);;
//		assertNotNull(mModelHelper);
//	}
//
//	public void testFindFirstMatchingPathToContent() {
//		PathToContent pathToContent = mModelHelper.findFirstMatchingPathToContent("http://vnexpress.net/tin-tuc/thoi-su/ha-noi-don-dep-day-dien-cap-chang-chit-tren-pho-2998401.html");
//		assertNotNull(pathToContent);
//		assertEquals("vn", pathToContent.getLanguage());
//	}
//
//	public void testGetFeedItem() throws IOException, FeedParserException {
//		Feeds feeds = mContentParser.parseFeeds("http://vnexpress.net/rss/thoi-su.rss", "utf-8");
//		assertTrue(feeds.size() > 0);
//		FeedItem feedItem = feeds.get(0);
//
//		assertTrue(feedItem.getImageUrl().startsWith("http://"));
//		Article article = mModelHelper.get(feedItem, null);
//		assertNotNull(article.getContent());
//	}
//
//	public void testGetTags() {
//		TreeSet<String> tags = mModelHelper.getTags();
//		System.out.println(tags);
//		assertTrue(tags.size() > 0);
//	}
//
//	public void testGetSubscriptions() throws IOException, FeedParserException {
//		List<Subscription> subscriptions = mModelHelper.getActiveSubscriptions("World");
//		assertTrue(subscriptions.size() > 0);
//	}
//}
