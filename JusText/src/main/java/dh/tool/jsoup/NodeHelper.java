package dh.tool.jsoup;

import com.google.common.base.Function;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;

import java.util.HashMap;

/**
 * Created by hiep on 24/06/2014.
 */
public class NodeHelper {
	public static Node nearestCommonAncestor(Node node1, Node node2) {
		Node ancestor=node1;
		while (ancestor!=null) {
			if (isAncestor(ancestor, node2)) {
				return ancestor;
			}
			ancestor = ancestor.parent();
		}
		throw new IllegalStateException("node1 and node2 do not have common ancestor");
	}

	/**
	 * return true if node1 is ancestor of node2 or node1 == node2
	 */
	public static boolean isAncestor(Node node1, Node node2) {
		if (node1 == node2) {
			return true;
		}
		Node ancestor=node2;

		while (ancestor!=null) {
			if (ancestor == node1) {
				return true;
			}
			ancestor = ancestor.parent();
		}

		return false;
	}

	/**
	 * return true if node has ancestor satisfy f
	 */
	public static boolean ancestorSatisfy(Node node, Function<Node, Boolean> f) {
		Node ancestor=node;

		while (ancestor!=null) {
			if (f.apply(ancestor)) {
				return true;
			}
			ancestor = ancestor.parent();
		}

		return false;
	}

	/**
	 * return true if node has a heading ancestor
	 */
	public static boolean isHeading(Node node) {
		Node ancestor=node;

		while (ancestor!=null) {
			if (isHeadingTag(ancestor)) {
				return true;
			}
			ancestor = ancestor.parent();
		}

		return false;
	}

	/**
	 * return true if node has a link ancestor
	 */
	public static boolean isLink(Node node) {
		Node ancestor=node;

		while (ancestor!=null) {
			if (isLinkTag(ancestor)) {
				return true;
			}
			ancestor = ancestor.parent();
		}

		return false;
	}



	public static enum TagType {IGNORABLE, INNERTEXT, BLOCKLEVEL, BLOCKLEVEL_CONTENT, BLOCKLEVEL_TITLE}

	public static final HashMap<String, TagType> TagsType = new HashMap<String, TagType>(){{
		put("style", TagType.IGNORABLE);
		put("script", TagType.IGNORABLE);
		put("option", TagType.IGNORABLE);
		put("noscript", TagType.IGNORABLE);
		put("embed", TagType.IGNORABLE);
		put("applet", TagType.IGNORABLE);
		put("link", TagType.IGNORABLE);
		put("head", TagType.IGNORABLE);
		put("button", TagType.IGNORABLE);
		put("select", TagType.IGNORABLE);
		put("input", TagType.IGNORABLE);
		put("textarea", TagType.IGNORABLE);
		put("keygen", TagType.IGNORABLE);
		put("blockquote", TagType.BLOCKLEVEL);
		put("caption", TagType.BLOCKLEVEL);
		put("center", TagType.BLOCKLEVEL);
		put("col", TagType.BLOCKLEVEL);
		put("colgroup", TagType.BLOCKLEVEL);
		put("dd", TagType.BLOCKLEVEL);
		put("div", TagType.BLOCKLEVEL);
		put("dl", TagType.BLOCKLEVEL);
		put("dt", TagType.BLOCKLEVEL);
		put("fieldset", TagType.BLOCKLEVEL);
		put("form", TagType.BLOCKLEVEL);
		put("legend", TagType.BLOCKLEVEL);
		put("optgroup", TagType.BLOCKLEVEL);
		put("option", TagType.BLOCKLEVEL);
		put("p", TagType.BLOCKLEVEL);
		put("pre", TagType.BLOCKLEVEL_CONTENT);
		put("table", TagType.BLOCKLEVEL);
		put("td", TagType.BLOCKLEVEL);
		put("textarea", TagType.BLOCKLEVEL);
		put("tfoot", TagType.BLOCKLEVEL);
		put("th", TagType.BLOCKLEVEL);
		put("thead", TagType.BLOCKLEVEL);
		put("tr", TagType.BLOCKLEVEL);
		put("ul", TagType.BLOCKLEVEL);
		put("li", TagType.BLOCKLEVEL);
		put("h1", TagType.BLOCKLEVEL_TITLE);
		put("h2", TagType.BLOCKLEVEL_TITLE);
		put("h3", TagType.BLOCKLEVEL_TITLE);
		put("h4", TagType.BLOCKLEVEL_TITLE);
		put("h5", TagType.BLOCKLEVEL_TITLE);
		put("h6", TagType.BLOCKLEVEL_TITLE);
		put("code", TagType.BLOCKLEVEL_CONTENT); //main content for sure
		put("b", TagType.INNERTEXT); //count as text inside block
		put("u", TagType.INNERTEXT); //count as text inside block
		put("i", TagType.INNERTEXT);//count as text inside block
		put("br", TagType.INNERTEXT); //count as text inside block
		//put("img", TagType.INNERTEXT_ONLY); //count as text inside block
	}};

	public static boolean isIgnorableTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return TagsType.get(tag.nodeName()) == TagType.IGNORABLE;
	}
	public static boolean isBlockTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return ((Element)tag).isBlock();
		/*TagType type = TagsType.get(tag.toLowerCase());
		return type == TagType.BLOCKLEVEL || type == TagType.BLOCKLEVEL_CONTENT || type == TagType.BLOCKLEVEL_TITLE;*/
	}
	public static boolean isInlineTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return ((Element)tag).tag().isInline();
		/*TagType type = TagsType.get(tag.toLowerCase());
		return type == TagType.INNERTEXT;*/
	}
	public static boolean isHeadingTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return TagsType.get(tag.nodeName()) == TagType.BLOCKLEVEL_TITLE;
	}

	public static boolean isLinkTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return "a".equalsIgnoreCase(tag.nodeName());
	}

	public static boolean isImgTag(Node tag) {
		if (tag == null || !(tag instanceof Element)) {
			return false;
		}
		return "img".equalsIgnoreCase(tag.nodeName());
	}

	public static boolean isIgnorableTagNode(Node node) {
		return node instanceof Comment || isIgnorableTag(node);
	}

}
