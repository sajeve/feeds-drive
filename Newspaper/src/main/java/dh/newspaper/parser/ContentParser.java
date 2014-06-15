package dh.newspaper.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import android.webkit.URLUtil;
import dh.newspaper.Constants;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.Feeds;
import dh.newspaper.tools.thread.ICancellation;
import dh.newspaper.tools.NetworkUtils;
import dh.newspaper.tools.StrUtils;
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
	private static final String TAG = ContentParser.class.getName();

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
			charSet = Constants.DEFAULT_ENCODING;
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

	//<editor-fold desc="Feed Parse">

	public Feeds parseFeeds(String addressUrl, String charSet, ICancellation cancelListener) throws FeedParserException, IOException {
		InputStream input = NetworkUtils.getStreamFromUrl(addressUrl, NetworkUtils.DESKTOP_USER_AGENT, cancelListener);
		if (input==null) {
			return null;
		}
		Document doc = Jsoup.parse(input, charSet, addressUrl, Parser.xmlParser());
		input.close();
		return parseFeeds(doc);
	}

	public Feeds parseFeeds(Document doc) throws FeedParserException {
		FeedFormat feedFormat;

		Elements itemsElem = doc.select("rss>channel>item");
		int itemCount = itemsElem==null ? 0 :  itemsElem.size();
		if (itemCount > 0) {
			feedFormat = FeedFormat.RSS;
		}
		else {
			feedFormat = FeedFormat.ATOM;
			itemsElem = doc.select("feed>entry");
			itemCount = itemsElem==null ? 0 : itemsElem.size();
		}
		if (itemCount==0) {
			throw new FeedParserException(doc.baseUri(), "Cannot parse: '"+ StrUtils.ellipsize(doc.text(), 50)+"'");
		}

		String feedsLanguage = parseFeedsLanguage(feedFormat, doc);

		Feeds  resu = new Feeds(itemCount,
					doc.baseUri(),
					feedsLanguage,
					parseFeedsDescription(feedFormat, doc),
					parseFeedsPublishedDate(feedFormat, doc)
				);
		for (int i = 0; i < itemCount; i++) {
			Element elem = itemsElem.get(i);
			FeedItem item = new FeedItem(
					doc.baseUri(),
					parseItemTitle(elem, i, feedFormat),
					parseItemPublishDate(elem, i, feedFormat),
					parseItemDescription(elem, i, feedFormat),
					parseItemUri(elem, i, feedFormat),
					parseItemLanguage(elem, feedFormat, feedsLanguage),
					parseItemAuthor(elem, i, feedFormat)
			);
			item.initImageAndExcerpt();
			resu.add(item);
		}

		return resu;
	}
	//</editor-fold>

	//<editor-fold desc="Rss Parse Item details">

	private String parseFeedsLanguage(FeedFormat feedFormat, Document doc) {
		switch (feedFormat) {
			case RSS: {
				return getFirstText(doc, "rss>channel>language");
			}
			case ATOM: {
				return null;
			}
		}
		return null;
	}
	private String parseFeedsDescription(FeedFormat feedFormat, Document doc) throws FeedParserException {
		String feedsDescription = null;
		switch (feedFormat) {
			case RSS: {
				feedsDescription = getFirstText(doc, "rss>channel>description");
				if (Strings.isNullOrEmpty(feedsDescription)) {
					feedsDescription = getFirstText(doc, "rss>channel>title");
				}
				break;
			}
			case ATOM: {
				feedsDescription = getFirstText(doc, "feed>title");
				break;
			}
		}

		if (Strings.isNullOrEmpty(feedsDescription)) {
			throw new FeedParserException(doc.baseUri(), "Description is empty 'rss>channel>description' or 'feed>title'");
		}

		return feedsDescription;
	}
	private String parseFeedsPublishedDate(FeedFormat feedFormat, Document doc) throws FeedParserException {
		switch (feedFormat) {
			case RSS: {
				return getFirstText(doc, "rss>channel>pubDate");
			}
			case ATOM: {
				return getFirstText(doc, "feed>updated");
			}
		}

		/*
			throw new FeedParserException("Unable to find published Date from 'rss>channel>pubDate' or 'feed>updated'", 0);
		}*/

		return null;
	}

	private String parseItemUri(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String uri = null;
		switch (feedFormat) {
			case RSS: {
				uri = getFirstText(elem, "guid");
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				uri = getFirstText(elem, "link");
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				break;
			}
			case ATOM: {
				uri = getFirstText(elem, "id");
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				uri = getFirstAttr(elem, "link", "href");
				if (URLUtil.isValidUrl(uri)) {
					return uri;
				}
				break;
			}
		}

		throw new FeedParserException(elem.baseUri(), "Unable to find URI in <guid> or <link> tag '"+uri+"'", pos);
	}

	private String parseItemTitle(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String title = getFirstText(elem, "title");
		if (Strings.isNullOrEmpty(title)) {
			throw new FeedParserException(elem.baseUri(), "<title> is empty", pos);
		}
		return title;
	}

	private String parseItemPublishDate(Element elem, int pos, FeedFormat feedFormat){
		String pubDateStr = null;
		switch (feedFormat) {
			case RSS: {
				return getFirstText(elem, "pubDate");
			}
			case ATOM: {
				return getFirstText(elem, "published");
			}
		}

		/*if (Strings.isNullOrEmpty(pubDateStr)) {
			throw new FeedParserException(elem.baseUri(), "<pubDate> is empty", pos);
		}*/
		return null;
	}

	private String parseItemDescription(Element elem, int pos, FeedFormat feedFormat) throws FeedParserException {
		String description = null;
		switch (feedFormat) {
			case RSS: {
				description = getFirstText(elem, "description");
				break;
			}
			case ATOM: {
				description = getFirstText(elem, "content");
				break;
			}
		}

		/*if (Strings.isNullOrEmpty(description)) {
			throw new FeedParserException(elem.baseUri(), "<description> is empty", pos);
		}*/
		return description;
	}

	/**
	 * Optional
	 * @return
	 */
	private String parseItemAuthor(Element elem, int pos, FeedFormat feedFormat) {
		switch (feedFormat) {
			case RSS: {
				return getFirstText(elem, "author");
			}
			case ATOM: {
				return getFirstText(elem, "author>name");
			}
		}
		return null;
	}

	private String parseItemLanguage(Element elem, FeedFormat feedFormat, String feedsLang) {
		String lang = null;
		switch (feedFormat) {
			case RSS: {
				//lang = getFirstText(elem, "language");
				lang = null;
				break;
			}
			case ATOM: {
				lang = elem.attr("xml:lang");
				break;
			}
		}

		return Strings.isNullOrEmpty(lang) ? feedsLang : lang;
	}

	//</editor-fold>

	private String getFirstText(Document doc, String xpath) {
		String textValue;
		Elements elems = doc.select(xpath);
		if (elems == null) {
			return null;
		}
		Element elem = elems.first();
		if (elem == null) {
			return null;
		}
		return elem.text();
	}

	private String getFirstText(Element e, String xpath) {
		String textValue;
		Elements elems = e.select(xpath);
		if (elems == null) {
			return null;
		}
		Element elem = elems.first();
		if (elem == null) {
			return null;
		}
		return elem.text();
	}

	private String getFirstAttr(Element e, String xpath, String attr) {
		Elements elems = e.select(xpath);
		if (elems == null) {
			return null;
		}
		if (elems == null) {
			return null;
		}
		Element elem = elems.first();
		if (elem == null) {
			return null;
		}
		return elem.attr(attr);
	}
}
