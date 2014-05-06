package dh.newspaper.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;
import dh.newspaper.parser.NetworkUtils;
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
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;

public class RomeRssReaderTest extends ActivityUnitTestCase<MainActivity> {

	private final static String TAG = RomeRssReaderTest.class.getName();

  /*  public RomeRssReaderTest(String name) {
        super();
    }
*/

    public RomeRssReaderTest() {
        super(MainActivity.class);
    }
    /*public RomeRssReaderTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }*/

	public void testAsync() throws Exception {
		String address = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml";

		AsyncTask<String, Void, Document> loadPage = new AsyncTask<String, Void, Document>() {
			@Override
			protected Document doInBackground(String...params) {
				try {
					String address = params[0];
					InputStream input = NetworkUtils.getStreamFromUrl(address);
					return Jsoup.parse(input, "UTF-8", address, Parser.xmlParser());
				} catch (IOException e) {
					//e.printStackTrace();
					Log.w(TAG, e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Document doc) {
				doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
				//doc.normalise();
				System.out.println(doc.html());
				Log.i(TAG, doc.html());
			}
		};
		loadPage.execute(address);
	}

    public void testRetreiveFeeds() throws Exception {
		//URL feedUrl = new URL("http://www.huffingtonpost.com/feeds/verticals/education/news.xml");
		String address = "http://www.huffingtonpost.com/feeds/verticals/education/news.xml";
		InputStream input = NetworkUtils.getStreamFromUrl2(address);
		Document doc = Jsoup.parse(input, "UTF-8", address, Parser.xmlParser());

		doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		//doc.normalise();
		System.out.println(doc.html());
		Log.i(TAG, doc.html());
/*
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(feedUrl));
		Assert.assertTrue("Read feed OK", feed != null);
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
		Assert.assertFalse("Feed seems valid", Strings.isNullOrEmpty(resu.get(0)));*/
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
