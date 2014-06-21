package de.l3s.boilerpipe.demo;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.ImageExtractor;
import org.junit.Test;

/**
 * Demonstrates how to use Boilerpipe to get the images within the main content.
 * 
 * @author Christian Kohlschütter
 */
public final class ImageExtractorTest {

	public static void main(String[] args) throws Exception {
		URL url = new URL(
				"http://ngoisao.net/tin-tuc/phong-cach/thoi-trang/hoa-tiet-trai-cay-vui-nhon-goi-he-3006796.html");
		
		// choose from a set of useful BoilerpipeExtractors...
		final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.DEFAULT_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.CANOLA_EXTRACTOR;
//		final BoilerpipeExtractor extractor = CommonExtractors.LARGEST_CONTENT_EXTRACTOR;

		final ImageExtractor ie = ImageExtractor.INSTANCE;
		
		List<Image> imgUrls = ie.process(url, extractor);
		
		// automatically sorts them by decreasing area, i.e. most probable true positives come first
		Collections.sort(imgUrls);
		
		for(Image img : imgUrls) {
			System.out.println("* "+img);
		}

	}
}
