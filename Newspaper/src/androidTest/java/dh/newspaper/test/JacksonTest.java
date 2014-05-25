//package dh.newspaper.test;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import dh.newspaper.model.json.JsonPathToContent;
//import dh.newspaper.model.json.JsonSubscription;
//import junit.framework.TestCase;
//import org.joda.time.DateTime;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
//public class JacksonTest extends TestCase {
//	private static final String CATEGORY_FILE = "/mnt/shared/bridge/category.json";
//	private static final String PATHTOCONTENT_FILE = "/mnt/shared/bridge/pathToContent.json";
//	private static final String SUBSCRIPTION_FILE = "/mnt/shared/bridge/subscription.json";
//
//	ObjectMapper mapper = null;
//
//	@Override
//	protected void setUp() throws Exception {
//		super.setUp();
//		mapper = new ObjectMapper();
//	}
//	private void writeJson() throws IOException {
//		{
//			File f = new File(PATHTOCONTENT_FILE);
//			mapper.writerWithDefaultPrettyPrinter().writeValue(f, new JsonPathToContent[]{
//					new JsonPathToContent("*vnexpress.net/*", "div.short_intro, div.relative_new, div.fck_detail", "vn"),
//					new JsonPathToContent("*nytimes.com/*", "div.article-body", "en-US"),
//					new JsonPathToContent("*huffingtonpost.com/*", "div.article>p"),
//					new JsonPathToContent("*cnn.com/*", "div.articleContent") {{setEnable(false);}}
//			});
//		}
//		{
//			File f = new File(SUBSCRIPTION_FILE);
//
//			JsonSubscription[] data = new JsonSubscription[]{
//					new JsonSubscription("http://www.huffingtonpost.com/feeds/verticals/chicago/news.xml", Arrays.asList(1L, 3L), "en-US"),
//					new JsonSubscription("http://vnexpress.net/rss/thoi-su.rss", Arrays.asList(1L), "vn"),
//					new JsonSubscription("http://rss.nytimes.com/services/xml/rss/nyt/AsiaPacific.xml", Arrays.asList(3L), "vn"),
//					new JsonSubscription("http://rss.nytimes.com/services/xml/rss/nyt/Travel.xml", Arrays.asList(2L), "en-US"),
//					new JsonSubscription("http://vnexpress.net/rss/tin-moi-nhat.rss"),
//					new JsonSubscription("http://rss.cnn.com/rss/edition_world.rss", Arrays.asList(3L)) {{setEnable(false);}}
//			};
//
//			//mapper.writeValue(f, data);
//			mapper.writerWithDefaultPrettyPrinter().writeValue(f, data);
//		}
//	}
//	private void readJson() throws IOException {
//		{
//			File f = new File(PATHTOCONTENT_FILE);
//			List<JsonPathToContent> pathToContents = mapper.readValue(f, new TypeReference<List<JsonPathToContent>>() {
//			});
//			assertEquals(4, pathToContents.size());
//			assertEquals("*vnexpress.net/*", pathToContents.get(0).getUrlPattern());
//			assertEquals("vn", pathToContents.get(0).getLanguage());
//			assertTrue( pathToContents.get(1).isEnable());
//			assertFalse( pathToContents.get(3).isEnable());
//		}
//		{
//			File f = new File(SUBSCRIPTION_FILE);
//			List<JsonSubscription> subscriptions = mapper.readValue(f, new TypeReference<List<JsonSubscription>>() {
//			});
//			assertEquals(6, subscriptions.size());
//			assertEquals("http://www.huffingtonpost.com/feeds/verticals/chicago/news.xml", subscriptions.get(0).getFeedsUrl());
//			assertEquals(1L, subscriptions.get(0).getTags().get(0).longValue());
//			assertEquals(3L, subscriptions.get(0).getTags().get(1).longValue());
//			assertEquals("en-US", subscriptions.get(0).getLanguage());
//			assertTrue( subscriptions.get(0).isEnable());
//			assertEquals(0L, subscriptions.get(4).getTags().get(0).longValue());
//			assertFalse( subscriptions.get(5).isEnable());
//		}
//	}
//	public void testWriteReadJson() throws IOException {
//		writeJson();
//		readJson();
//	}
//
////	public void testRequiredProp() throws IOException {
////		File f = new File("/mnt/shared/bridge/category_error.json");
////
////		try {
////			List<JsonCategory> categories = mapper.readValue(f, new TypeReference<List<JsonCategory>>() {
////			});
////			mapper.writerWithDefaultPrettyPrinter().writeValue(new File("/mnt/shared/bridge/category_error_ok.json") , categories);
////			fail("Expected exception but not");
////		} catch (IOException e) {
////			e.printStackTrace();
////		}
////	}
//
//	public void testJodan() {
//		System.out.print(DateTime.now());
//	}
//}
