package de.l3s.boilerpipe.demo;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.Media;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HTMLHighlighter;
import de.l3s.boilerpipe.sax.HtmlArticleExtractor;
import de.l3s.boilerpipe.sax.ImageExtractor;
import de.l3s.boilerpipe.sax.MediaExtractor;
import dh.tool.TestUtils;
import org.junit.Test;

/**
 * @author manuel.codiga@gmail.com
 */
public final class HtmlArticleExtractorTest {
	@Test
	public void testHtmlArticleExtractor() throws Exception {
		//URL url = new URL("file:///D:/dev-news/genymotion/bridge/test/gm-recalls-3-million-more-cars.html");
		//String output = "nyt1.html";

		URL url = new URL("http://ngoisao.net/tin-tuc/phong-cach/thoi-trang/hoa-tiet-trai-cay-vui-nhon-goi-he-3006796.html");
		String output = "ngoisao1.html";

		String html = HtmlArticleExtractor.INSTANCE.process(CommonExtractors.ARTICLE_EXTRACTOR, url);

		TestUtils.writeToFile(output, html, true);
	}

	@Test
	public void testImageExtractor() throws Exception {
		URL url = new URL(
				"http://ngoisao.net/tin-tuc/phong-cach/thoi-trang/hoa-tiet-trai-cay-vui-nhon-goi-he-3006796.html");

		// choose from a set of useful BoilerpipeExtractors...
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.DEFAULT_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.CANOLA_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.LARGEST_CONTENT_EXTRACTOR;
//		final ImageExtractor ie = ImageExtractor.INSTANCE;

		final HTMLHighlighter hh = HTMLHighlighter.newExtractingInstance();
		PrintWriter out = new PrintWriter("ngoisao2.html", "UTF-8");
		out.println("<base href=\"" + url + "\" >");
		out.println("<meta http-equiv=\"Content-Type\" content=\"text-html; charset=utf-8\" />");
		String extractedHtml = hh.process(url, CommonExtractors.ARTICLE_EXTRACTOR);
		out.println(extractedHtml);
		out.close();

//		List<Image> imgUrls = ie.process(url, extractor);
//
//		// automatically sorts them by decreasing area, i.e. most probable true positives come first
//		Collections.sort(imgUrls);
//
//		for(Image img : imgUrls) {
//			System.out.println("* "+img);
//		}

	}
}
