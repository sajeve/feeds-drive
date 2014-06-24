package dh.tool.jsoup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by hiep on 24/06/2014.
 */
public class NodeHelperTest {
	@Test
	public void testAncestor() {
		Document doc = Jsoup.parse("<a><b><c/><e/></b><d/></a>");
		Node nodeA = doc.select("a").first();
		Node nodeB = doc.select("b").first();
		Node nodeC = doc.select("c").first();
		Node nodeD = doc.select("d").first();
		Node nodeE = doc.select("e").first();

		Assert.assertTrue(NodeHelper.isAncestor(nodeA, nodeB));
		Assert.assertTrue(NodeHelper.isAncestor(nodeA, nodeC));
		Assert.assertTrue(NodeHelper.isAncestor(nodeA, nodeD));
		Assert.assertTrue(NodeHelper.isAncestor(nodeA, nodeE));
		Assert.assertTrue(NodeHelper.isAncestor(nodeB, nodeC));
		Assert.assertTrue(NodeHelper.isAncestor(nodeC, nodeC));
		Assert.assertFalse(NodeHelper.isAncestor(nodeB, nodeD));
		Assert.assertFalse(NodeHelper.isAncestor(nodeC, nodeD));
		Assert.assertFalse(NodeHelper.isAncestor(nodeC, nodeE));

		Assert.assertEquals(nodeA, NodeHelper.nearestCommonAncestor(nodeB, nodeD));
		Assert.assertEquals(nodeB, NodeHelper.nearestCommonAncestor(nodeC, nodeE));
		Assert.assertEquals(nodeB, NodeHelper.nearestCommonAncestor(nodeB, nodeE));
		Assert.assertEquals(nodeA, NodeHelper.nearestCommonAncestor(nodeE, nodeD));
	}
}
