package com.sree.textbytes.readabilityBUNDLE;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.sree.textbytes.StringHelpers.StopWords;
import com.sree.textbytes.StringHelpers.StopwordsManager;
import com.sree.textbytes.StringHelpers.StringSplitter;
import com.sree.textbytes.StringHelpers.string;
import com.sree.textbytes.readabilityBUNDLE.cleaner.DocumentCleaner;
import com.sree.textbytes.readabilityBUNDLE.extractor.GooseExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilityExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilitySnack;
import com.sree.textbytes.readabilityBUNDLE.formatter.DocumentFormatter;
import com.sree.textbytes.readabilityBUNDLE.image.BestImageGuesser;
import dh.tool.common.PerfWatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Original code from Project Goose
 * 
 * modified author sree
 *
 */

public class ContentExtractor {

	public static enum Algorithm {ReadabilitySnack, ReadabilityCore, ReadabilityGoose}

	public static Logger logger = LoggerFactory.getLogger(ContentExtractor.class.getName());

	public Article extractContent(String rawHtml, List<String> htmlSources, String baseUri, Algorithm extractionAlgorithm, String lang) {
		return performExtraction(htmlSources, rawHtml, baseUri, extractionAlgorithm, lang);
	}
	
	public Article extractContent(String rawHtml, String baseUri, Algorithm extractionAlgorithm, String lang) {
		List<String> htmlSources = new ArrayList<String>();
		htmlSources = null;
		return performExtraction(htmlSources, rawHtml, baseUri, extractionAlgorithm, lang);
	}

	private Article performExtraction(List<String> htmlSources, String rawHtml, String baseUri, Algorithm algorithm, String lang) {
		PerfWatcher pf = new PerfWatcher(logger, algorithm + " - " + baseUri);

		Article article = new Article();
		article.setRawHtml(rawHtml);

		Document document = Jsoup.parse(rawHtml, baseUri);
		pf.d("Jsoup parse");

		if (Strings.isNullOrEmpty(lang)) {
			lang = detectLanguage(document);
			pf.d("detect language");
		}

		if (!Strings.isNullOrEmpty(lang)) {
			try {
				lang = StopwordsManager.getLanguage(lang);
				StopwordsManager.loadStopwords(lang);
				article.setLanguage(lang);
				pf.d("load stopwords");
			}
			catch (Exception ex) {
				lang = null;
				pf.d("load stopwords", ex);
			}
		}

		try {
			article.setPublishDate(extractPublishedDate(document));
			pf.d("detect publish date");
		}catch(Exception e) {
			pf.d("Publish Date extraction failed", e);
		}

		try {
			article.setTags(extractTags(document));
			pf.d("detect tags");
		}catch(Exception e) {
			pf.d("Extract tags failed", e);
		}

		try {
			article.setTitle(getTitle(document));
			pf.d("detect title");
		} catch (Exception e) {
			pf.d("Extract title failed", e);
		}

		try {
			article.setMetaDescription(getMetaDescription(document));
			article.setMetaKeywords(getMetaKeywords(document));
			pf.d("set meta description and keywords");
		} catch (Exception e) {
			pf.d("set meta description and keywords", e);
		}

		/**
		 * Find out the possibility of Next Page in the input,
		 */
		if(htmlSources != null) {
			if(htmlSources.size() > 0) {
				logger.trace("There are next pages, true with size : "+htmlSources.size());
				article.setMultiPage(true);
				article.setNextPageHtmlSources(htmlSources);
			}
		}

		pf.resetStopwatch();

		//now perform a nice deep cleansing
		DocumentCleaner documentCleaner = new DocumentCleaner();
		document = documentCleaner.clean(document, pf);
		pf.d("Cleaned Document");

		article.setCleanedDocument(document);

		switch (algorithm) {
			case ReadabilityCore:
				ReadabilityExtractor readabilityCore = new ReadabilityExtractor();
				article.setTopNode(readabilityCore.grabArticle(article, lang, pf));
				break;
			case ReadabilityGoose:
				GooseExtractor gooseExtractor = new GooseExtractor();
				article.setTopNode(gooseExtractor.grabArticle(article, lang, pf));
				break;
			default:
				ReadabilitySnack readabilitySnack = new ReadabilitySnack();
				article.setTopNode(readabilitySnack.grabArticle(article, lang, pf));
				break;
		}

		pf.d("grabArticle");

		if(article.getTopNode() != null) {
			/**
			 * Check out another Image Extraction algorithm to find out the best image
			 */

			try {
				BestImageGuesser bestImageGuesser = new BestImageGuesser();
				bestImageGuesser.filterBadImages(article.getTopNode());

				Elements imgElements = article.getTopNode()
						.getElementsByTag("img");
				ArrayList<String> imageCandidates = new ArrayList<String>();
				for (Element imgElement : imgElements) {
					imageCandidates.add(imgElement.attr("src"));

				}
				pf.d("Available size of images in top node : "+ imageCandidates.size());

				if(imageCandidates.size() > 0) {
					pf.trace("Top node has images " + imageCandidates.size());
				}else {
					article.setTopImage(bestImageGuesser.getTopImage(article.getTopNode(), document));
					pf.d("BestImage : "	+ article.getTopImage().getImageSrc());

					String bestImage = article.getTopImage().getImageSrc();
					if (!string.isNullOrEmpty(bestImage)) {
						pf.trace("Best image found : " + bestImage);
						if(!imageCandidates.contains(bestImage)) {
							pf.trace("Top node does not contain the same Best Image");
							try {
								if(article.getTopNode().children().size() > 0) {
									pf.trace("Child Nodes greater than Zero "+article.getTopNode().children().size());
									article.getTopNode().child(0).before("<p><img src=" + bestImage + "></p>");
								} else {
									pf.trace("Top node has 0 childs appending after");
									article.getTopNode().append("<p><img src=" + bestImage + "></p>");
								}

							} catch (Exception e) {
								pf.d("Find best image", e);
							}

						}else {
							pf.trace("Top node already has the Best image found");
						}
					}
				}
			} catch (Exception e) {
				pf.d("Best Image Guesser failed ", e);
			}

			/**
			 * So we have all of the content that we need. Now we clean it up for presentation.
			 **/

			DocumentFormatter documentFormatter = new DocumentFormatter();
			Element node = documentFormatter.getFormattedElement(article.getTopNode());

			article.setCleanedArticleText(outputNormalization(node.toString()));

			/**
			 * check whether the extracted content lenght less than meta
			 * description
			 */
			pf.trace("Meta des length : "+ article.getMetaDescription().length()+ "content length : "+ article.getTopNode().text().length());
			if (article.getMetaDescription().trim().length() > article.getTopNode().text().length()) {
				pf.trace("Meta Description greater than extracted content , swapping");
				article.setCleanedArticleText("<div><p>"+ article.getMetaDescription().trim() + "</p></div>");
			}
		}
		pf.dg("extracted");
		return article;
	}
	
	/**
	 * Convert single Brs in to double brs
	 * @param text
	 * @return
	 */
	
	private String outputNormalization(String text) {
		return text.replaceAll("<br[^>]*>", "<br /><br />");
	}
	
	
	/**
	 * if the article has meta keywords set in the source, use that
	 */
	private String getMetaKeywords(Document doc) {
		return getMetaContent(doc, "meta[name=keywords]");
	}
	
	/**
	 * if the article has meta description set in the source, use that
	 */
	private String getMetaDescription(Document doc) {
		return getMetaContent(doc, "meta[name=description]");
	}
	
	private String getMetaContent(Document doc, String metaName) {
		Elements meta = doc.select(metaName);
		if (meta.size() > 0) {
			String content = meta.first().attr("content");
			return string.isNullOrEmpty(content) ? string.empty : content.trim();
		}
		return string.empty;
	}


	public static String detectLanguage(Document doc) {
		Element htmlTag = doc.select("html").first();
		if (htmlTag.attributes().hasKey("lang")) {
			return htmlTag.attr("lang");
		}
		if (htmlTag.attributes().hasKey("xml:lang")) {
			return htmlTag.attr("xml:lang");
		}
		return null;
	}

	/**
	 * attemps to grab titles from the html pages, lots of sites use different
	 * delimiters for titles so we'll try and do our best guess.
	 * 
	 * 
	 * @param doc
	 * @return
	 */
	private String getTitle(Document doc) {
		String title = string.empty;

		Elements titleElem = doc.getElementsByTag("title");
		if (titleElem == null || titleElem.isEmpty())
			return string.empty;

		String titleText = titleElem.first().text();
		if (string.isNullOrEmpty(titleText))
			return string.empty;

		boolean usedDelimeter = false;

		if (titleText.contains("|")) {
			titleText = doTitleSplits(titleText, Patterns.PIPE_SPLITTER);
			usedDelimeter = true;
		}

		if (!usedDelimeter && titleText.contains("-")) {
			titleText = doTitleSplits(titleText, Patterns.DASH_SPLITTER);
			usedDelimeter = true;
		}
		if (!usedDelimeter && titleText.contains("»")) {
			titleText = doTitleSplits(titleText, Patterns.ARROWS_SPLITTER);
			usedDelimeter = true;
		}

		if (!usedDelimeter && titleText.contains(":")) {
			titleText = doTitleSplits(titleText, Patterns.COLON_SPLITTER);
		}

		// encode unicode charz
		title = HtmlEscapers.htmlEscaper().escape(titleText);
		title = Patterns.MOTLEY_REPLACEMENT.replaceAll(title);

		return title;
	}
	
	/**
	 * based on a delimeter in the title take the longest piece or do some
	 * custom logic based on the site
	 * 
	 * @param title
	 * @param splitter
	 * @return
	 */
	private String doTitleSplits(String title, StringSplitter splitter) {
		int largetTextLen = 0;
		int largeTextIndex = 0;

		String[] titlePieces = splitter.split(title);

		// take the largest split
		for (int i = 0; i < titlePieces.length; i++) {
			String current = titlePieces[i];
			if (current.length() > largetTextLen) {
				largetTextLen = current.length();
				largeTextIndex = i;
			}
		}

		return Patterns.TITLE_REPLACEMENTS.replaceAll(titlePieces[largeTextIndex])
				.trim();
	}
	
	private Set<String> extractTags(Element node) {
		if (node.children().size() == 0)
			return Patterns.NO_STRINGS;
		Elements elements = Selector.select(Patterns.A_REL_TAG_SELECTOR, node);
		if (elements.size() == 0)
			return Patterns.NO_STRINGS;
		Set<String> tags = new HashSet<String>(elements.size());
		for (Element el : elements) {
			String tag = el.text();
			if (!string.isNullOrEmpty(tag))
				tags.add(tag);
		}
		return tags;
	}
	
	private String extractPublishedDate(Document doc) {
		String pubDateRegex = "(DATE|date|pubdate|Date|REVISION_DATE)";
		return doc.select("meta[name~="+pubDateRegex+"]").attr("content");
	}
	
	
	// used for gawker type ajax sites with pound sites
	private String getUrlToCrawl(String urlToCrawl) {
		String finalURL;
		if (urlToCrawl.contains("#!")) {
			finalURL = Patterns.ESCAPED_FRAGMENT_REPLACEMENT.replaceAll(urlToCrawl);
		} else {
			finalURL = urlToCrawl;
		}
		//logger.trace("Extraction: " + finalURL);
		return finalURL;
	}
}
