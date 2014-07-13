package com.sree.textbytes.readabilityBUNDLE.nextpage;

import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;
import com.sree.textbytes.readabilityBUNDLE.cleaner.DocumentCleaner;
import com.sree.textbytes.readabilityBUNDLE.extractor.GooseExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilityExtractor;
import com.sree.textbytes.readabilityBUNDLE.extractor.ReadabilitySnack;
import com.sree.textbytes.readabilityBUNDLE.formatter.DocumentFormatter;
import dh.tool.common.PerfWatcher;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Append next page extracted content and create a final consolidated
 * 
 * @author sree
 *
 */

public class AppendNextPage {
	
	public Logger logger = LoggerFactory.getLogger(AppendNextPage.class.getName());
	
	public List<Integer> contentHashes = new ArrayList<Integer>();
	
	/**
	 * Append next page content 
	 * 
	 * @param article
	 * @param firstPageContent
	 * @param extractionAlgo
	 * @return
	 */
	public Element appendNextPageContent(Article article, Element firstPageContent, ContentExtractor.Algorithm extractionAlgo, String lang, PerfWatcher pf) {
		int pageNumber = 1;

		DocumentFormatter documentFormatter = new DocumentFormatter();
		
		contentHashes.add(firstPageContent.text().hashCode());
		Document document = article.getCleanedDocument();
		document.body().empty();
		
		Element finalConsolidatedContent = document.createElement("div").attr("id", "ace-final-consolidated");
		Element articleContent = document.createElement("div").attr("algo-page-number", Integer.toString(pageNumber)).attr("class", "algo-page-class");
		articleContent.appendChild(documentFormatter.getFormattedElement(firstPageContent));
		
		finalConsolidatedContent.appendChild(articleContent);
		
		DocumentCleaner documentClearner = new DocumentCleaner();
		
		if(article.isMultiPage()) {
			List<String> nextPageHtmlSource = article.getNextPageSources();

			for(String nextPageHtml : nextPageHtmlSource) {
				pf.resetStopwatch();

				Element nextPageExtractedContent = null;
				Document nextPageDocument = nextPageDocument = Jsoup.parse(nextPageHtml);

				pf.d("Fetching article from next page");

				nextPageDocument = documentClearner.clean(nextPageDocument, pf);

				pf.d("Clean");

				switch (extractionAlgo) {
					case ReadabilityCore:
						ReadabilityExtractor readabilityCore = new ReadabilityExtractor();
						nextPageExtractedContent = readabilityCore.fetchArticleContent(nextPageDocument, lang);
						break;
					case ReadabilityGoose:
						GooseExtractor readabilityGoose = new GooseExtractor();
						nextPageExtractedContent = readabilityGoose.fetchArticleContent(nextPageDocument, lang);
						break;
					default:
						ReadabilitySnack readabilitySnack = new ReadabilitySnack();
						nextPageExtractedContent = readabilitySnack.fetchArticleContent(nextPageDocument, lang);
						break;
				}

				pf.d("fetchArticleContent");

				if(nextPageExtractedContent != null) {
					if(checkDuplicateNextPage(nextPageExtractedContent.text().hashCode())) {
						logger.trace("Duplicate next page content found , skipping");
					}else {
						
						contentHashes.add(nextPageExtractedContent.text().hashCode());
						Element nextPageContent = document.createElement("div").attr("algo-page-number", Integer.toString(pageNumber)).attr("class", "algo-page-class");
						nextPageContent.appendChild(documentFormatter.getFormattedElement(nextPageExtractedContent));
						//logger.trace("Next Page Content : "+nextPageExtractedContent);
						if (!checkParagraphDeDupe(finalConsolidatedContent,nextPageContent)) {
							finalConsolidatedContent.appendChild(nextPageContent);
							pageNumber++;
						}
					}
				}
			}
		}
		
		return finalConsolidatedContent;
	}
	
	
	/**
	 * Paragraph duplicate mechanism. Check whether next page extracted content is duplicate of existing.
	 * 
	 * @param finalConsolidatedContent
	 * @param nextPageContent
	 * @return
	 */
	
	private boolean checkParagraphDeDupe(Element finalConsolidatedContent,Element nextPageContent)
    {

		int pSize = totalTags(nextPageContent);
		if(pSize==0)
		{
		 return true;
		}
		int i = 0, finalPSize = 0;
		Element firstPara = nextPageContent.getElementsByTag("p").get(i);
		if (firstPara.toString().length() < 100) {
			if (pSize > 1) {
				i = 1;
				firstPara = nextPageContent.getElementsByTag("p").get(i);
			}
		}
		Elements finalElements = finalConsolidatedContent
				.getElementsByAttribute("algo-page-number");
		for (Element elt : finalElements) {
			finalPSize = totalTags(elt);
			if (finalPSize > i) {
				Element firstPtag = elt.getElementsByTag("p").get(i);
				if (firstPara.toString().equals(firstPtag.toString())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private int totalTags(Element element)
	{
		return element.getElementsByTag("p").size();
	}
	
	/**
	 * De dupe mechanism using content hash
	 * 
	 * @param contentHash
	 * @return
	 */
	private boolean checkDuplicateNextPage(int contentHash) {
		if(contentHashes.contains(contentHash)) {
			return true;
		}else 
			return false;
	}
}
