package dh.newspaper.model;

import dh.newspaper.R;

/**
 * Created by hiep on 15/05/2014.
 */
public class FakeDataProvider {
	public static String[] getCategories() {
		return new String[] {
				"VnExpress",
				"Newyork Times",
				"Huffing Post",
		};
	}

	public static String getCategorySource(int categoryId) {
		switch (categoryId) {
			case 0: return "http://vnexpress.net/rss/tin-moi-nhat.rss";
			case 1: return "http://www.nytimes.com/services/xml/rss/nyt/AsiaPacific.xml";
			case 2: return "http://www.huffingtonpost.com/tag/asian-americans/feed";
			default: return null;
		}
	}
}
