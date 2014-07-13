package dh.tool.jsoup;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
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

	@Test
	public void testEmptyElements() {
		Document doc = Jsoup.parse("<a><b><c/><e/></b><d/></a><img/>");
		Node nodeA = doc.select("a").first();
		Node nodeB = doc.select("b").first();
		Node nodeC = doc.select("c").first();
		Node nodeD = doc.select("d").first();
		Node nodeE = doc.select("e").first();
		Node nodeImg = doc.select("img").first();
		Assert.assertFalse(NodeHelper.isEmptyElement(nodeA));
		Assert.assertFalse(NodeHelper.isEmptyElement(nodeImg));
		Assert.assertTrue(NodeHelper.isEmptyElement(nodeD));
	}

	@Test
	public void testRemoveEmptyElements() {
		Document doc = Jsoup.parse("<a><b><c/><e/></b><d/></a><img/>");
		NodeHelper.cleanEmptyElements(doc);
		System.out.println(doc);
	}

	@Test
	public void testWrap() {
		Document doc = Jsoup.parse("<textarea>hello</textarea>");
		Element e = doc.select("textarea").first();
		Parser.parseFragment("<span></span>", e, "localhost");
	}

	@Test
	public void testUnwrapUniqueTags() {
		Document doc = Jsoup.parse("<section><section><div><div>hello</div></div>world</section></section>");
		NodeHelper.unwrapRedundancyTags(doc);
		System.out.println(doc.html());
	}

	@Test
	public void testGetUniqueLeafTag() {
		{
			Document doc = Jsoup.parse("<html><section><div><div>hello</div></div>world</section></html>");
			Assert.assertNull(NodeHelper.getUniqueLeafTag(doc.select("section").first()));
		}
		{
			Document doc = Jsoup.parse("<html><section><div><div2>hello</div2></div></section></html>");
			Assert.assertEquals("div2", NodeHelper.getUniqueLeafTag(doc.select("section").first()).nodeName());
		}
		{
			Document doc = Jsoup.parse("<html><foo><bar><img/></bar></foo><section><div><div2>hello</div2></div></section><li>haha</li></html>");
			System.out.println(doc.html());

			Assert.assertEquals("li", NodeHelper.getUniqueLeafTag(doc.select("li").first()).nodeName());
			Assert.assertEquals("img", NodeHelper.getUniqueLeafTag(doc.select("foo").first()).nodeName());
			Assert.assertEquals("div2", NodeHelper.getUniqueLeafTag(doc.select("section").first()).nodeName());
			Assert.assertNull(NodeHelper.getUniqueLeafTag(doc.select("html").first()));
		}
	}
}
