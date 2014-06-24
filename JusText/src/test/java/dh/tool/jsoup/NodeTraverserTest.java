package dh.tool.jsoup;

import com.google.common.base.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.junit.Test;

/**
 * Created by hiep on 24/06/2014.
 */
public class NodeTraverserTest {
	@Test
	public void testTraversor() {
		Document document = Jsoup.parse("<a><b>haha</b><c><d/></c></a>");
		new NodeTraversor(new NodeVisitor() {
			@Override
			public void head(Node node, int depth) {
				for (int i = 0; i < node.childNodes().size(); i++) {
					Node child = node.childNode(i);

					//remove useless Tags
					if (child.nodeName().equalsIgnoreCase("c")) {
						child.remove();
					}
				}
				System.out.println(Strings.repeat("  ", depth)+node.nodeName());
			}

			@Override
			public void tail(Node node, int depth) {
				System.out.println(Strings.repeat("  ", depth)+"/"+node.nodeName());
			}
		}).traverse(document);

	}

	@Test
	public void testXpathRecursive() {
		Document document = Jsoup.parse("<div>hehe<div>haha</div></div>");
		System.out.println(document.select("div").toString());
	}
}
