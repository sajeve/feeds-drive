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

		//assertEquals("http://www.huffingtonpost.com/kiran-ahuja/reflecting-on-50-years-of_b_5553462.html", feedItem.getUri());
		SelectArticleWorkflow saw = new SelectArticleWorkflow(this.getActivity(), feedItem, Constants.ARTICLE_TTL, true, null);
		saw.run();
		assertTrue(!Strings.isNullOrEmpty(feedItem.getTextPlainDescription()));
	}

	public void testSelectArticleWorkflowOffline() throws IOException, FeedParserException {
		Feeds feeds = mContentParser.parseFeeds("http://rss.nytimes.com/services/xml/rss/nyt/Science.xml", "utf-8", null);
		assertTrue(feeds.size() > 0);
		FeedItem feedItem = feeds.get(0);

		//assertEquals("http://www.huffingtonpost.com/kiran-ahuja/reflecting-on-50-years-of_b_5553462.html", feedItem.getUri());
		Log.i("TEST", "Start test "+feedItem.getUri());
		SelectArticleWorkflow saw = new SelectArticleWorkflow(this.getActivity(), feedItem, Constants.ARTICLE_TTL, false, null);
		saw.run();
		assertTrue(!Strings.isNullOrEmpty(feedItem.getTextPlainDescription()));
	}


	public void testSelectArticleWorkflow2() throws IOException, FeedParserException {

		AppContextModule appContextModule = new AppContextModule(this.getActivity());
		DaoSession daoSession = appContextModule.provideDaoSession();
		Article article = daoSession.getArticleDao().loadByRowId(2778);

//		QueryBuilder<Subscription> subscriptionByTagQueryBuilder = daoSession.getSubscriptionDao().queryBuilder()
//				.where(SubscriptionDao.Properties.Tags.like("%|" + tag.toUpperCase() + "|%")
//						, SubscriptionDao.Properties.Enable.eq(Boolean.TRUE));
//		Query<Subscription> subscriptionByTagQuery = subscriptionByTagQueryBuilder.build();
//		LazyList<Subscription> subscriptions = subscriptionByTagQuery.listLazy();


		SelectArticleWorkflow saw = new SelectArticleWorkflow(this.getActivity(), article, Constants.ARTICLE_TTL, true, null);
		saw.run();
	}

}