package dh.tool.justext;

import dh.tool.jsoup.NodeHelper;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.security.InvalidParameterException;
import java.util.LinkedList;

/**
 * Traverse document to enumerate paragraphs block using Block Tags
 * see {@link #getParagraphs()}
 * Created by hiep on 24/06/2014.
 */
class ParagraphsExplorer implements NodeVisitor {
	public enum AncestorState {INNERTEXT_ONLY, BLOCKLEVEL, UNKNOW};
	private final LinkedList<Paragraph> paragraphs = new LinkedList<Paragraph>();
	private final Configuration conf;

	public ParagraphsExplorer(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public void head(Node node, int depth) {
		if (node.childNodeSize() == 0) {
			if (node instanceof TextNode && StringUtil.isBlank(node.outerHtml()))
				return;
			mergeToResult(node);
		}
	}

	@Override
	public void tail(Node node, int depth) {

	}

	/**
	 * Get the paragraphs after visiting the document
	 */
	public LinkedList<Paragraph> getParagraphs() {
		return paragraphs;
	}

	private void mergeToResult(Node node) {
		Node lastAddedNode = getLastAddedNode();
		if (lastAddedNode==null) {
			insertAsNewParagraph(node);
			return;
		}

		AncestorState ancestorState = getAncestorState(lastAddedNode, node);
		switch (ancestorState) {
			case BLOCKLEVEL: insertAsNewParagraph(node);
				return;
			case INNERTEXT_ONLY: appendToLastParagraph(node);
				return;
		}
	}

	/**
	 * Visit from lastNode and currentNode to the first common ancestor of these 2 nodes,
	 * - if all the visited ancestors are {@link dh.tool.jsoup.NodeHelper.TagType#INNERTEXT} returns {@link ParagraphsExplorer.AncestorState#INNERTEXT_ONLY}
	 * - if one of the visited ancestors is {@link dh.tool.jsoup.NodeHelper#isBlockTag(org.jsoup.nodes.Node)} returns {@link ParagraphsExplorer.AncestorState#BLOCKLEVEL}
	 * - otherwise returns {@link ParagraphsExplorer.AncestorState#UNKNOW}
	 */
	public static AncestorState getAncestorState(Node lastNode, Node currentNode) {
		if (lastNode==null || currentNode==null)
			throw new InvalidParameterException();

		Node ancestor = NodeHelper.nearestCommonAncestor(lastNode, currentNode);
		AncestorState as1 = getAncestorStateOfBranch(ancestor, lastNode);
		if (as1 == AncestorState.BLOCKLEVEL) {
			return AncestorState.BLOCKLEVEL;
		}
		AncestorState as2 = getAncestorStateOfBranch(ancestor, currentNode);
		if (as2 == AncestorState.BLOCKLEVEL) {
			return AncestorState.BLOCKLEVEL;
		}
		if (as1==AncestorState.INNERTEXT_ONLY && as2==AncestorState.INNERTEXT_ONLY) {
			return AncestorState.INNERTEXT_ONLY;
		}
		return AncestorState.UNKNOW;
	}

	private void insertAsNewParagraph(Node node) {
		paragraphs.add(new Paragraph(node, paragraphs.size(), conf));
	}
	private void appendToLastParagraph(Node node) {
		paragraphs.getLast().add(node);
	}

	private Node getLastAddedNode() {
		if (paragraphs.isEmpty()) {
			return null;
		}
		return paragraphs.getLast().getLast();
	}

	/**
	 * Visit from node to the ancestor
	 * - if all the visited ancestors are {@link NodeHelper.TagType#INNERTEXT} returns {@link ParagraphsExplorer.AncestorState#INNERTEXT_ONLY}
	 * - if one of the visited ancestors is {@link NodeHelper#isBlockTag(org.jsoup.nodes.Node)} returns {@link ParagraphsExplorer.AncestorState#BLOCKLEVEL}
	 * - otherwise returns {@link ParagraphsExplorer.AncestorState#UNKNOW}
	 */
	private static AncestorState getAncestorStateOfBranch(Node ancestor, Node node) {
		if (!NodeHelper.isAncestor(ancestor, node)) {
			throw new InvalidParameterException("ancestor pre-condition violation");
		}
		if (node == ancestor) {
			if (NodeHelper.isBlockTag(node)) return AncestorState.BLOCKLEVEL;
			if (NodeHelper.isInlineTag(node)) return AncestorState.INNERTEXT_ONLY;
			return AncestorState.UNKNOW;
		}
		Node n = node.parent();
		boolean innerTextOnly = true;
		while (n!=ancestor && n!=null) {
			if (NodeHelper.isBlockTag(n))
				return AncestorState.BLOCKLEVEL;
			if (!NodeHelper.isInlineTag(n)) {
				innerTextOnly = false;
			}
			n = n.parent();
		}
		return innerTextOnly ? AncestorState.INNERTEXT_ONLY : AncestorState.UNKNOW;
	}


}
