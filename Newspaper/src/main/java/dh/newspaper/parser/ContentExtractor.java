package dh.newspaper.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.webkit.URLUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.google.common.base.Strings;

/**
 * Clean a HTML, extract the main content
 * @author hiep
 */
public class ContentExtractor {
	/**
	 * Get the simplify format of the article from a Webpage:
	 * <ul>
	 * <li>Connect to address with mobile userAgent</li>
	 * <li>Use the mainContentQuery to extract article body</li>
	 * <li>Clean the html content</li>
	 * </ul>
	 *
	 * @see {@link #extract(Document, String)}
	 * @see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>
	 *
	 * @param addressUrl
	 * @param mainContentQuery (see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>)
	 * @return simplify content (keep only basic tag)
	 * @throws IOException
	 */
	public static Elements extract(String addressUrl, String mainContentQuery) throws IOException {
		Document doc = Jsoup.connect(addressUrl).userAgent(NetworkUtils.MOBILE_USER_AGENT).get();
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		return extract(doc, mainContentQuery);
	}

	/**
	 * Get the simplify format of the article from a InputStream:
	 * See {@link #extract(String, String)}
	 * @param charSet Encoding - default UTF-8 (if null)
	 */
	public static Elements extract(InputStream input, String charSet, String mainContentQuery, String baseURI) throws IOException {
		if (Strings.isNullOrEmpty(charSet)) {
			charSet = "UTF-8";
		}
		Document doc = Jsoup.parse(input, charSet, baseURI);
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		return extract(doc, mainContentQuery);
	}

	/**
	 * Get the simplify format of the article from a jsoup Document:
	 * <ul>
	 * <li>Use the mainContentQuery to extract article body</li>
	 * if  mainContentQuery == null: take the body part
	 * <li>Clean the html content</li>
	 * </ul>
	 *
	 * @param mainContentQuery (see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>)
	 * @return simplify content (keep only basic tag)
	 *
	 * @see {@link #extract(String, String)}
	 * @see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>
	 */
	public static Elements extract(Document doc, String mainContentQuery) {
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		Element body = doc.body();
		if (Strings.isNullOrEmpty(mainContentQuery)) {
			return new Elements(cleanHtml(body));
		}

		Elements elems = body.select(mainContentQuery);
		for(Element e : elems) {
			cleanHtml(e);
		}

		return elems;
	}

	/**
	 * Remove
	 * - script
	 * @see {@link #cleanUselessContent(Node)}
	 */
	private static Element cleanHtml(Element mainContent) {
		mainContent.select("script, style, link").remove(); //remove all script tags + contents
		mainContent.select("span").unwrap(); //remove all span tags
		cleanUselessContent(mainContent);
		return mainContent;
	}

	/**
	 * Remove all
	 * - comments
	 * - class properties
	 * @param node
	 */
	private static void cleanUselessContent(Node node) {

		for (int i = 0; i < node.childNodes().size();) {
            Node child = node.childNode(i);
            if (child instanceof Comment || child instanceof FormElement)
                child.remove();
            else {
            	cleanUselessContent(child);
            	i++;
            }

            Attributes atts = child.attributes();
            for (Attribute a : atts) {
            	if (a.getKey().equalsIgnoreCase("class"))
                child.removeAttr(a.getKey());
            }
        }
    }

	public static List<RssItem> parseRss(String addressUrl, String charSet) throws RssParserException, IOException {
		InputStream input = NetworkUtils.getStreamFromUrl2(addressUrl);
		Document doc = Jsoup.parse(input, charSet, addressUrl, Parser.xmlParser());
		input.close();
		return extractRssItem(doc);
	}

	public static List<RssItem> extractRssItem(Document doc) throws RssParserException {
		Elements itemsElem = doc.select("rss>channel>item");

		int itemCount = itemsElem.size();
		List<RssItem> resu = new ArrayList<>(itemCount);
		for (int i=0; i<itemCount; i++) {
			Element elem = itemsElem.get(i);
			RssItem rssItem = new RssItem(
					parseTitle(elem, i),
					parsePublishDate(elem, i),
					parseDescription(elem, i),
					parseUri(elem, i)
				);
			rssItem.author = parseAuthor(elem, i);
			resu.add(rssItem);
		}

		return resu;
	}

	private static String parseUri(Element elem, int pos) throws RssParserException {
		String uri = elem.select("guid").first().text();
		if (URLUtil.isValidUrl(uri)) {
			return uri;
		}
		uri = elem.select("link").first().text();
		if (URLUtil.isValidUrl(uri)) {
			return uri;
		}
		throw new RssParserException("Unable to find URI in <guid> or <link> tag", pos);
	}

	private static String parseTitle(Element elem, int pos) throws RssParserException {
		String title = elem.select("title").first().text();
		if (Strings.isNullOrEmpty(title)) {
			throw new RssParserException("<title> is empty", pos);
		}
		return title;
	}

	private static String parsePublishDate(Element elem, int pos) throws RssParserException {
		String pubDate = elem.select("pubDate").first().text();
		if (Strings.isNullOrEmpty(pubDate)) {
			throw new RssParserException("<pubDate> is empty", pos);
		}
		return pubDate;
	}
	private static String parseDescription(Element elem, int pos) throws RssParserException {
		String description = elem.select("description").first().text();
		if (Strings.isNullOrEmpty(description)) {
			throw new RssParserException("<description> is empty", pos);
		}
		return description;
	}

	/**
	 * Optional
	 * @return
	 */
	private static String parseAuthor(Element elem, int pos) {
		return elem.select("author").first().text();
	}

}
