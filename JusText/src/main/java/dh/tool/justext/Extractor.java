package dh.tool.justext;

import com.google.common.base.Strings;
import dh.tool.jsoup.NodeHelper;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.*;

public class Extractor {
	Configuration conf;

	public Extractor() {
		this(Configuration.DEFAULT);
	}
	public Extractor(Configuration conf) {
		this.conf = conf;
	}

	List<Paragraph> paragraphs;

	public String parse(Document document) {
		document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		cleanUselessContent(document);
		paragraphs = computeParagraphs(document);
		return null;
	}

	public List<Paragraph> getParagraphs() {
		return paragraphs;
	}

	/**
	 * @deprecated Replaced by {@link #cleanUselessContent(org.jsoup.nodes.Node)}
	 * Remove all comments, useless tag (script, style..), useless attributes (class)
	 */
	public static void cleanUselessContentNonOptimize(Node node) {
		for (int i = 0; i < node.childNodes().size(); i++) {
			Node child = node.childNode(i);

			//remove useless Tags
			if (NodeHelper.isIgnorableTagNode(child)) {
				child.remove();
			} else {
				cleanUselessContent(child);
				i++;
			}

			//remove useless Attributes
			Attributes atts = child.attributes();
			for (Attribute a : atts) {
				if (a.getKey().equalsIgnoreCase("class"))
					child.removeAttr(a.getKey());
			}
		}
	}

	/**
	 * Clean useless tags, attributes and replace absolute path
	 * @param node
	 */
	public static void cleanUselessContent(Node node) {
		new NodeTraversor(new NodeVisitor() {
			@Override
			public void head(Node node, int depth) {
				for (int i = 0; i < node.childNodes().size();) {
					Node child = node.childNode(i);

					//remove useless Tags
					if (NodeHelper.isIgnorableTagNode(child)) {
						child.remove();
					}
					else {
						i++;
					}
				}
				//remove useless Attributes
				Attributes atts = node.attributes();
				for (Attribute a : atts) {
					if (a.getKey().equalsIgnoreCase("class"))
						node.removeAttr(a.getKey());
				}

				//convert to absolute path
				if (NodeHelper.isLinkTag(node)) {
					String absolutePath = node.attr("abs:href");
					if (!Strings.isNullOrEmpty(absolutePath)) {
						node.attr("href", absolutePath);
					}
				} else if (NodeHelper.isImgTag(node)) {
					String absolutePath = node.attr("abs:src");
					if (!Strings.isNullOrEmpty(absolutePath)) {
						node.attr("src", absolutePath);
					}
				}
			}

			@Override
			public void tail(Node node, int depth) {

			}
		}).traverse(node);
	}

	private List<Paragraph> computeParagraphs(Node node) {
		ParagraphsExplorer pe = new ParagraphsExplorer(conf);
		node.traverse(pe);
		return pe.getParagraphs();
	}
}
