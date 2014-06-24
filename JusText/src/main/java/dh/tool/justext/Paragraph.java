package dh.tool.justext;

import dh.tool.jsoup.NodeHelper;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.LinkedList;

/**
 * A block of text node (leaf node, no-children nodes).
 * In the HTML document these nodes are place next to each other, separated only by inline tags (b, i, u, span..).
 * They often forms a paragraphs in the document.
 * Created by hiep on 24/06/2014.
 */
public class Paragraph extends LinkedList<Node> {
	private Configuration conf;

	private Quality quality;
	private String rawText;
	private int linksLength;
	private double linkDensity;
	private boolean isHeading;
	private double stopswordDensity;
	private String message;

	public Paragraph(Node firstNode, Configuration conf) {
		this.conf = conf;
		add(firstNode);
	}

	/**
	 * Perform context-free classification
	 */
	private Quality computeQuality() {
		int rawTextLength = rawText.length();

		if (linkDensity > conf.getMaxLinkDensity()) {
			message = String.format("Too much links density=%g > %g", linkDensity, conf.getMaxLinkDensity());
			return Quality.BAD;
		}

		if (rawText.contains("©") || rawText.contains("&copy;")) {
			message = "Contains copyright symbol";
			return Quality.BAD;
		}

		//short block
		if (rawTextLength < conf.getLengthLow()) {
			if (linkDensity > 0) {
				message = String.format("Short block (length=%d < %d) with links density=%g > 0", rawTextLength, conf.getLengthLow(), linkDensity);
				return Quality.BAD;
			}
			else
				message = String.format("Too short (length=%d < %d) to make decision", rawTextLength, conf.getLengthLow());
				return Quality.SHORT;
		}



		//medium and long block
		if (stopswordDensity >= conf.getStopwordsHigh()) {
			if (rawTextLength > conf.getLengthHigh()) {
				return Quality.GOOD;
			}
			else {
				message = String.format("High stop words (density=%g > %g) on medium block (length=%d < %d)", stopswordDensity, conf.getStopwordsHigh(), rawTextLength, conf.getLengthHigh());
				return Quality.NEAR_GOOD;
			}
		}

		if (stopswordDensity >= conf.getStopwordsLow()) {
			message = String.format("Medium/High stop words (density=%g > %g) on paragraph)", stopswordDensity, conf.getStopwordsHigh());
			return Quality.NEAR_GOOD;
		}
		else {
			message = String.format("Low stop words (density=%g < %g) on paragraph)", stopswordDensity, conf.getStopwordsLow());
			return Quality.BAD;
		}
	}

	public String getRawText() {
		initRawInfo();
		return rawText;
	}

	public double getStopswordDensity() {
		initRawInfo();
		return stopswordDensity;
	}

	public Quality getQuality() {
		initRawInfo();
		return quality;
	}

	public int getLinksLength() {
		initRawInfo();
		return linksLength;
	}

	public double getLinkDensity() {
		initRawInfo();
		return linkDensity;
	}

	public boolean isHeading() {
		initRawInfo();
		return isHeading;
	}

	/**
	 * Perform context-free classification. Compute only once.
	 * - {@link #rawText}
	 * - {@link #isHeading}
	 * - {@link #linkDensity}
	 * - {@link #quality}
	 * The second invocation of this method won't do anything
	 */
	public void initRawInfo() {
		if (rawText != null) {
			return;
		}

		isHeading = false;
		linksLength = 0;

		StringBuilder sb = new StringBuilder();
		for (Node n : this) {
			String nodeRawText;
			if (n instanceof TextNode)
				nodeRawText = ((TextNode) n).text();
			else
				nodeRawText = n.outerHtml();

			sb.append(nodeRawText);

			if (NodeHelper.isLink(n))
				linksLength += nodeRawText.length();

			/*
			if one of node in the fragment is heading, so the fragment is heading
			example: <h1><span>hello</span> world <img/></h1>
			 */
			if (!isHeading) {
				if (NodeHelper.isHeading(n)) {
					isHeading = true;
				}
			}
		}
		rawText = sb.toString();
		linkDensity = (double)linksLength / rawText.length();
		quality = computeQuality();
	}

	public void colorizeParagraph() {
		initRawInfo();

		String color = "white";
		switch (quality) {
			case BAD: color="red"; break;
			case GOOD: color="green"; break;
			case SHORT: color="yellow"; break;
			case NEAR_GOOD: color="LightGreen"; break;
		}

		for (Node n : this) {
			n.wrap(String.format("<span style=\"background:%s\" title=\"%s\"></span>", color, message));
		}
	}

	/**
	 * Quality of content
	 */
	public static enum Quality {
		BAD, //boilerplate
		GOOD,
		SHORT, //too short to make a reliable decision
		NEAR_GOOD, //somewhere in-between short and good
	}

}

