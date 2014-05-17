package dh.newspaper.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.webkit.URLUtil;
import dh.newspaper.model.FeedItem;
import org.jsoup.Connection;
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

import javax.inject.Inject;

/**
 * Clean a HTML, extractContent the main content
 * @author hiep
 */
public class ContentParser {

	public enum FeedFormat {RSS, ATOM};

	@Inject
	public ContentParser() {
	}

	/**
	 * Get the simplify format of the article from a Webpage:
	 * <ul>
	 * <li>Connect to address with mobile userAgent</li>
	 * <li>Use the mainContentQuery to extractContent article body</li>
	 * <li>Clean the html content</li>
	 * </ul>
	 *
	 * @see {@link #extractContent(Document, String)}
	 * @see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>
	 *
	 * @param addressUrl
	 * @param mainContentQuery (see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>)
	 * @return simplify content (keep only basic tag)
	 * @throws IOException
	 */
	public Elements extractContent(String addressUrl, String mainContentQuery) throws IOException {
		Connection connection = Jsoup.connect(addressUrl).userAgent(NetworkUtils.MOBILE_USER_AGENT);
		Document doc = connection.get();

		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		return extractContent(doc, mainContentQuery);
	}

	/**
	 * Get the simplify format of the article from a InputStream:
	 * See {@link #extractContent(String, String)}
	 * @param charSet Encoding - default UTF-8 (if null)
	 */
	public Elements extractContent(InputStream input, String charSet, String mainContentQuery, String baseURI) throws IOException {
		if (Strings.isNullOrEmpty(charSet)) {
			charSet = "UTF-8";
		}
		Document doc = Jsoup.parse(input, charSet, baseURI);
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		return extractContent(doc, mainContentQuery);
	}

	/**
	 * Get the simplify format of the article from a jsoup Document:
	 * <ul>
	 * <li>Use the mainContentQuery to extractContent article body</li>
	 * if  mainContentQuery == null: take the body part
	 * <li>Clean the html content</li>
	 * </ul>
	 *
	 * @param mainContentQuery (see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>)
	 * @return simplify content (keep only basic tag)
	 *
	 * @see {@link #extractContent(String, String)}
	 * @see <a href="http://jsoup.org/cookbook/extracting-data/selector-syntax">Jsoup Selector</a>
	 */
	public Elements extractContent(Document doc, String mainContentQuery) throws MalformedURLException {
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

	private void absolutePath(Element content) throws MalformedURLException {
		Elements imgElems = content.select("img");
		for (Element imgelem : imgElems) {
			String absolutePath = imgelem.attr("abs:src");
			if (!Strings.isNullOrEmpty(absolutePath)) {
				imgelem.attr("src", absolutePath);
			}
		}

		Elements linksElems = content.select("a");
		for (Element linkelem : linksElems) {
			String absolutePath = linkelem.attr("abs:href");
			if (!Strings.isNullOrEmpty(absolutePath)) {
				linkelem.attr("href", absolutePath);
			}
		}
	}

	/**
	 * Remove
	 * - script, style, link (to css, javascript)
	 * - convert image path to absolute
	 * @see {@link #cleanUselessContent(Node)}
	 */
	private Element cleanHtml(Element mainContent) throws MalformedURLException {
		mainContent.select("script, style, link").remove(); //remove all script tags + contents
		mainContent.select("span").unwrap(); //remove all span tags
		cleanUselessContent(mainContent);
		absolutePath(mainContent);
		return mainContent;
	}

	/**
	 * Remove all
	 * - comments
	 * - class properties
	 * @param node
	 */
	private void cleanUselessContent(Node node) {

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

	public List<FeedItem> parseRssUrl(String addressUrl, String charSet) throws FeedParserException, IOException {
		InputStream input = NetworkUtils.getStreamFromUrl(addressUrl, NetworkUtils.DESKTOP_USER_AGENT);
		Document doc = Jsoup.parse(input, charSet, addressUrl, Parser.xmlParser());
		input.close();
		return parseRssItems(doc);
	}

	public List<FeedItem> parseRssItems(Document doc) throws FeedParserException {
		FeedFormat feedFormat;

		Elements itemsElem = doc.select("rss>channel>item");
		int itemCount = itemsElem.size();
		if (itemCount > 0) {
			feedFormat = FeedFormat.RSS;
		}
		else {
			feedFormat = FeedFormat.ATOM;
			itemsElem = doc.select("feed>entry");
			itemCount = itemsElem.size();
		}
		if (itemCount==0) {
			throw new FeedParserException("Cannot parse '"+doc.baseUri()+"': '"+StrUtils.ellipsize(doc.text(), 50)+"'", 0);
		}

		List<FeedItem>  resu = new ArrayList<>(itemCount);
		for (int i = 0; i < itemCount; i++) {
			Element elem = itemsElem.get(i);
			FeedItem item = new FeedItem(
					parseItemTitle(elem, i, feedFormat),
					parseItemPublishDate(elem, i, feedFormat),
					parseRssDescription(elem, i, feedFormat),
					parseItemUri(elem, i, feedFormat)
			);
			item.author = parseItemAuthor(elem, i, feedFormat);
			resu.add(item);
		}

		return resu;
	}

	//<editor-fold desc="Rss Parse Item details">

	private String parseItemUri(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {

		switch (feedFormat) {
			case RSS: {
				String uri = elem.select("guid").first().text();
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				uri = elem.select("link").first().text();
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
			}
			case ATOM: {
				String uri = elem.select("id").first().text();
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				uri = elem.select("link").first().attr("href");
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
			}
		}

		throw new FeedParserException("Unable to find URI in <guid> or <link> tag", pos);
	}

	private String parseItemTitle(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String title = elem.select("title").first().text();
		if (Strings.isNullOrEmpty(title)) {
			throw new FeedParserException("<title> is empty", pos);
		}
		return title;
	}

	private String parseItemPublishDate(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String pubDate = null;
		switch (feedFormat) {
			case RSS: {
				pubDate = elem.select("pubDate").first().text();
				break;
			}
			case ATOM: {
				pubDate = elem.select("published").first().text();
				break;
			}
		}

		if (Strings.isNullOrEmpty(pubDate)) {
			throw new FeedParserException("<pubDate> is empty", pos);
		}
		return pubDate;
	}
	private String parseRssDescription(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String description = null;
		switch (feedFormat) {
			case RSS: {
				description = elem.select("description").first().text();
				break;
			}
			case ATOM: {
				description = elem.select("content").first().text();
				break;
			}
		}

		if (Strings.isNullOrEmpty(description)) {
			throw new FeedParserException("<description> is empty", pos);
		}
		return description;
	}

	/**
	 * Optional
	 * @return
	 */
	private String parseItemAuthor(Element elem, int pos, FeedFormat feedFormat) {
		switch (feedFormat) {
			case RSS: {
				Elements elemAuthor = elem.select("author");
				if (elemAuthor != null && !elemAuthor.isEmpty()) {
					return elemAuthor.first().text();
				}
			}
			case ATOM: {
				Elements elemAuthor = elem.select("author>name");
				if (elemAuthor != null && !elemAuthor.isEmpty()) {
					return elemAuthor.first().text();
				}
			}
		}
		return null;
	}

	//</editor-fold>

}
