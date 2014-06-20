package de.l3s.boilerpipe.demo;

import java.net.URL;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.Media;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HtmlArticleExtractor;
import de.l3s.boilerpipe.sax.MediaExtractor;
import dh.tool.TestUtils;
import org.junit.Test;

/**
 * @author manuel.codiga@gmail.com
 */
public final class HtmlArticleExtractorTest {
	@Test
	public void testDemo1() throws Exception {
		URL url = new URL("file:///D:/dev-news/genymotion/bridge/test/gm-recalls-3-million-more-cars.html");
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;

		final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
		
		String html = htmlExtr.process(extractor, url);

		TestUtils.writeToFile("nyt1.html", html, false);
	}
}
