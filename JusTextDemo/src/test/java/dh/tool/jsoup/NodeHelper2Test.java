package dh.tool.jsoup;

import dh.tool.justext.Extractor;
import dh.tool.justext.demo.util.Network;
import junit.framework.Assert;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by hiep on 24/06/2014.
 */
public class NodeHelper2Test {
	@Test
	public void testGetParent() throws IOException {
		Document doc = Network.downloadPage("http://edition.cnn.com/2014/07/11/sport/football/world-cup-pleitgen-german-words/index.html?hpt=hp_c2", false);
		System.out.println(NodeHelper.displayParent(doc.select("img[src*=140630153844]").first()));
	}

	@Test
	public void testCleanUselessContent() throws IOException {
		Document doc = Network.downloadPage("http://edition.cnn.com/2014/07/11/sport/football/world-cup-pleitgen-german-words/index.html?hpt=hp_c2", false);
		Assert.assertNotNull(doc.select("img[src*=140630153844]").first());
		new Extractor().cleanUselessContent(doc);
		Assert.assertNotNull(doc.select("img[src*=140630153844]").first());
	}
}
