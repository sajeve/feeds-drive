package dh.newspaper.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import android.test.ActivityUnitTestCase;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndEntry;
import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedInput;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.XmlReader;
import com.google.common.base.Strings;

import dh.newspaper.MainActivity;

public class RomeRssReaderTest extends ActivityUnitTestCase<MainActivity> {

	public RomeRssReaderTest(Class<MainActivity> activityClass) {
		super(activityClass);
		// TODO Auto-generated constructor stub
	}


	public void testRetreiveFeeds() throws Exception {
		URL feedUrl = new URL("http://www.huffingtonpost.com/feeds/verticals/education/news.xml");

		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(feedUrl));

		Assert.assertTrue("Read feed OK", feed != null);

		ArrayList<String> resu = new ArrayList<String>();

		for (Object o : feed.getEntries()) {
			SyndEntry entry = (SyndEntry)o;
			//System.out.println(entry);

			resu.add(entry.getTitle());

			System.out.println(entry.getTitle());
			System.out.println(entry.getPublishedDate());
			System.out.println(entry.getDescription().getType()); //if it is text/html
			System.out.println(entry.getDescription().getValue()); //so get image from the first <img> tag here
			System.out.println(entry.getUri());
			System.out.println("-------------");
		}

		Assert.assertTrue("Found feed", feed.getEntries().size()>0);
		Assert.assertFalse("Feed seems valid", Strings.isNullOrEmpty(resu.get(0)));
	}


	protected InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException
    {
		HttpGet httpGet_ = new HttpGet(address);
        HttpClient httpclient = new DefaultHttpClient();

        // Execute HTTP Get Request
        HttpResponse response = httpclient.execute(httpGet_);
        return response.getEntity().getContent();
    }

    public void testHtmlParser() throws Exception {
    	Document doc = Jsoup.connect("https://fr.news.yahoo.com/").get();


    	Assert.assertFalse(Strings.isNullOrEmpty(doc.title()));

    	System.out.println("-------------");
    	System.out.println(doc.body().text());
    	System.out.println("-------------");
    	System.out.println(doc.title());

    	//doc.outputSettings()
    	//System.out.println(doc.body().text());
	}


    public void testHtmlParserIgnoreScript() {

//    	Document doc = Jsoup.parse(R.string.html_sample_test1);
//
//    	Assert.assertFalse(Strings.isNullOrEmpty(doc.title()));
//    	System.out.println("-------------");
//    	System.out.println(doc.body().text());
//    	System.out.println("-------------");
//    	System.out.println(doc.title());

    }
}
