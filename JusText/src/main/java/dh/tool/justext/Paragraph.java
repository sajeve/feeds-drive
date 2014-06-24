package dh.tool.justext;

import dh.tool.jsoup.NodeHelper;
import org.jsoup.helper.StringUtil;
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
	private final Configuration conf;

	private Quality contextFreeQuality;
	/**
	 * context-sensitive quality
	 */
	private Quality quality;
	private String rawText;
	private int linksLength;
	private double linkDensity;
	private boolean isHeading;
	private double stopwordsDensity;
	private String message;

	public Paragraph(Node firstNode, Configuration conf) {
		this.conf = conf;
		add(firstNode);
	}

	/**
	 * Perform context-free classification
	 */
	private Quality computeContextFreeQuality() {
		int rawTextLength = rawText.length();

		if (linkDensity > conf.getMaxLinkDensity()) {
			if (Configuration.DEBUG)
				message = String.format("Context-free BAD: Too much links density=%.3g > %.3g", linkDensity, conf.getMaxLinkDensity());
			return Quality.BAD;
		}

		if (rawText.contains("©") || rawText.contains("&copy;")) {
			if (Configuration.DEBUG)
				message = "Context-free BAD: Contains copyright symbol";
			return Quality.BAD;
		}

		//short block
		if (rawTextLength < conf.getLengthLow()) {
			if (linkDensity > 0) {
				if (Configuration.DEBUG)
					message = String.format("Context-free BAD: Short paragraph (length=%d < %d) with links density=%.3g > 0", rawTextLength, conf.getLengthLow(), linkDensity);
				return Quality.BAD;
			}
			else {
				if (Configuration.DEBUG)
					message = String.format("Context-free SHORT: Too short (length=%d < %d) to make decision", rawTextLength, conf.getLengthLow());
				return Quality.SHORT;
			}
		}

		//medium and long block
		if (stopwordsDensity >= conf.getStopwordsHigh()) {
			if (rawTextLength > conf.getLengthHigh()) {
				if (Configuration.DEBUG)
					message = String.format("Context-free GOOD: High stop words (density=%.3g >= %.3g) on long paragraph (length=%d > %d)", stopwordsDensity, conf.getStopwordsHigh(), rawTextLength, conf.getLengthHigh());
				return Quality.GOOD;
			}
			else {
				if (Configuration.DEBUG)
					message = String.format("Context-free NEAR_GOOD: High stop words (density=%.3g >= %.3g) on medium block (length=%d <= %d)", stopwordsDensity, conf.getStopwordsHigh(), rawTextLength, conf.getLengthHigh());
				return Quality.NEAR_GOOD;
			}
		}

		if (stopwordsDensity >= conf.getStopwordsLow()) {
			if (Configuration.DEBUG)
				message = String.format("Context-free NEAR_GOOD: Medium/High stop words (density=%.3g >= %.3g) on paragraph)", stopwordsDensity, conf.getStopwordsHigh());
			return Quality.NEAR_GOOD;
		} else {
			if (Configuration.DEBUG)
				message = String.format("Context-free BAD: Low stop words (density=%.3g < %.3g) on paragraph)", stopwordsDensity, conf.getStopwordsLow());
			return Quality.BAD;
		}
	}

	public String getRawText() {
		initRawInfo();
		return rawText;
	}

	public double getStopwordsDensity() {
		initRawInfo();
		return stopwordsDensity;
	}

	public Quality getContextFreeQuality() {
		initRawInfo();
		return contextFreeQuality;
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

	public Quality getQuality() {
		if (quality == null) {
			return getContextFreeQuality();
		}
		return quality;
	}

	public void setQuality(Quality quality) {
		if (Configuration.DEBUG)
			if (message.startsWith("Context-sensitive ")) {
				message += " ";
			}
			message = "Context-sensitive "+quality+". "+message;
		this.quality = quality;
	}

	/**
	 * Perform context-free classification. Compute only once.
	 * - {@link #rawText}
	 * - {@link #isHeading}
	 * - {@link #linkDensity}
	 * - {@link #contextFreeQuality}
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
		contextFreeQuality = computeContextFreeQuality();
	}

	public boolean isNearOrShort() {
		return getQuality() == Quality.NEAR_GOOD || getQuality() == Quality.SHORT;
	}

	/**
	 * wrap all elements ({@link org.jsoup.nodes.Node}) of the paragraph by a span tag with background color
	 * indiquate the contextFreeQuality of content
	 */
	public void colorizeParagraph() {
		String color = "white";
		switch (getQuality()) {
			case BAD: color="red"; break;
			case GOOD: color="green"; break;
			case SHORT: color="yellow"; break;
			case NEAR_GOOD: color="LightGreen"; break;
		}

		for (Node n : this) {
			n.wrap(String.format("<span style=\"background:%s\" title=\"%s\"></span>", color, message));
		}
	}

	@Override
	public String toString() {
		if (Configuration.DEBUG) {
			return message;
		}
		return getQuality().toString();
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

