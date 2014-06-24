package dh.tool.justext;

import com.google.common.base.Stopwatch;
import dh.tool.TestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 24/06/2014.
 */
public class ParagraphsExplorerTest {
	@Test
	public void testGetAncestorState() {
		{
			Document doc = Jsoup.parse("<b><div>hello<u>dream</u></div></b> <b><i>world</i></b>");
			System.out.println(doc);

			Node nodeHello = doc.select("b div").first().textNodes().get(0);
			System.out.println(nodeHello);
			Assert.assertEquals("div", nodeHello.parent().nodeName());

			Node nodeWorld = doc.select("b i").first().textNodes().get(0);
			Assert.assertEquals("i", nodeWorld.parent().nodeName());

			Assert.assertEquals(ParagraphsExplorer.AncestorState.BLOCKLEVEL, ParagraphsExplorer.getAncestorState(nodeHello, nodeWorld));
		}
		{
			Document doc = Jsoup.parse("<div><b><i>hello<u>dream</u></i></b><b><i>world</i></b></div>");
			Node nodeDream = doc.select("b i u").first().textNodes().get(0);
			Node nodeWorld = doc.select("b i").first().textNodes().get(0);
			Assert.assertEquals(ParagraphsExplorer.AncestorState.INNERTEXT_ONLY, ParagraphsExplorer.getAncestorState(nodeDream, nodeWorld));
		}
	}

	@Test
	public void testGetParagraphs() {
		{
			Document doc = Jsoup.parse("<b><div>hello<u>dream</u></div></b><b><i>world</i></b>");
			Node nodeHello = doc.select("b div").first().textNodes().get(0);
			Node nodeWorld = doc.select("b i").first().textNodes().get(0);

			Extractor.cleanUselessContent(doc);
			ParagraphsExplorer pe = new ParagraphsExplorer(Configuration.DEFAULT);
			doc.traverse(pe);

			Assert.assertEquals(2, pe.getParagraphs().size());
			Assert.assertEquals(nodeHello, pe.getParagraphs().getFirst().getFirst());
			Assert.assertEquals("dream", pe.getParagraphs().getFirst().getLast().outerHtml());
			Assert.assertEquals(nodeWorld, pe.getParagraphs().getLast().getFirst());
		}
		{
			Document doc = Jsoup.parse("<div> <b> <i>hello<u>dream</u></i> </b>  <b> <i id='foo'>world</i> </b> </div>");
			Node nodeDream = doc.select("b i u").first().textNodes().get(0);
			Node nodeWorld = doc.select("b i#foo").first().textNodes().get(0);

			Extractor.cleanUselessContent(doc);
			ParagraphsExplorer pe = new ParagraphsExplorer(Configuration.DEFAULT);
			doc.traverse(pe);

			Assert.assertEquals(1, pe.getParagraphs().size());
			Assert.assertEquals("hello", pe.getParagraphs().getFirst().getFirst().outerHtml());
			Assert.assertEquals(nodeDream, pe.getParagraphs().getFirst().get(1));
			Assert.assertEquals(nodeWorld, pe.getParagraphs().getLast().getLast());
		}
	}

	@Test
	public void testFreeContextClassify() throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		Document document = Jsoup.parse(new URL("http://dantri.com.vn/phap-luat/phat-hien-chan-dong-hon-14000-so-dien-thoai-bi-nghe-len-theo-doi-891664.htm"), Integer.MAX_VALUE);
		System.out.println("Download and parse "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		TestUtils.writeToFile("vnexpress1-origin.html", document.html(), false);

		sw.reset().start();
		Extractor extractor = new Extractor();
		extractor.removeBoilerplate(document);
		System.out.println("Remove boilerplate "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		TestUtils.writeToFile("vnexpress1-final.html", document.html(), false);
	}
}
