package dh.tool.justext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import dh.tool.jsoup.NodeHelper;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;

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
	private int countWords;
	private int countStopwords;
	private double linkDensity;
	private boolean heading;
	private double stopwordsDensity;
	private String message;
	private int id;

	/**
	 * true only if we has stopwords list of the configured language
	 */
	private boolean stopwordsChecking = false;

	public Paragraph(Node firstNode, int id, Configuration conf) {
		this.id = id;
		this.conf = conf;
		add(firstNode);
	}

	/**
	 * Perform context-free classification
	 */
	private Quality computeContextFreeQuality() {
		int rawTextLength = rawText.length();
		double stopwordsHigh = stopwordsChecking ? conf.stopwordsHigh() : 0;
		double stopwordsLow = stopwordsChecking ? conf.stopwordsLow() : 0;
		String lang = Strings.isNullOrEmpty(conf.language()) ? "" : " "+conf.language();

		if (message == null) {
			message="";
		}

		if (linkDensity > conf.maxLinkDensity()) {
			if (Configuration.DEBUG)
				message = String.format("Context-free BAD: Too much links density=%.3g > %.3g. ",
						linkDensity, conf.maxLinkDensity()) + message;
			return Quality.BAD;
		}

		if (rawText.contains("©") || rawText.contains("&copy;")) {
			if (Configuration.DEBUG)
				message = "Context-free BAD: Contains copyright symbol"+message;
			return Quality.BAD;
		}

		//short block
		if (rawTextLength < conf.lengthLow()) {
			if (linkDensity > 0) {
				if (Configuration.DEBUG)
					message = String.format("Context-free BAD: Short paragraph (length=%d < %d) with links density=%.3g > 0. ",
							rawTextLength, conf.lengthLow(), linkDensity) + message;
				return Quality.BAD;
			}
			else {
				if (Configuration.DEBUG)
					message = String.format("Context-free SHORT: Too short (length=%d < %d) to make decision. ",
							rawTextLength, conf.lengthLow()) + message;
				return Quality.SHORT;
			}
		}

		//medium and long block
		if (stopwordsDensity >= stopwordsHigh) {
			if (rawTextLength > conf.lengthHigh()) {
				if (Configuration.DEBUG)
					message = String.format("Context-free GOOD: High stop words%s (density=%.3g >= %.3g) on long paragraph (length=%d > %d). ",
							lang, stopwordsDensity, stopwordsHigh, rawTextLength, conf.lengthHigh()) + message;
				return Quality.GOOD;
			}
			else {
				if (Configuration.DEBUG)
					message = String.format("Context-free NEAR_GOOD: High stop words%s (density=%.3g >= %.3g) on medium block (length=%d <= %d). ",
							lang, stopwordsDensity, stopwordsHigh, rawTextLength, conf.lengthHigh()) + message;
				return Quality.NEAR_GOOD;
			}
		}

		if (stopwordsDensity >= stopwordsLow) {
			if (Configuration.DEBUG)
				message = String.format("Context-free NEAR_GOOD: Medium/High stop words%s (density=%.3g >= %.3g) on paragraph",
						lang, stopwordsDensity, stopwordsHigh) + message;
			return Quality.NEAR_GOOD;
		} else {
			if (Configuration.DEBUG)
				message = String.format("Context-free BAD: Low stop words%s (density=%.3g < %.3g) on paragraph",
						lang, stopwordsDensity, stopwordsLow) + message;
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

	public double getLinkDensity() {
		initRawInfo();
		return linkDensity;
	}

	public int getLength() {
		initRawInfo();
		return rawText.length();
	}

	public boolean isHeading() {
		initRawInfo();
		return heading;
	}

	public Quality getQuality() {
		if (quality == null) {
			return getContextFreeQuality();
		}
		return quality;
	}

	void setQuality(Quality quality, String reason) {
		if (Configuration.DEBUG) {
			message = reason+" " + quality + ". " + message;
		}
		this.quality = quality;
	}

	void setContextFreeQuality(Quality contextFreeQuality, String reason) {
		if (Configuration.DEBUG) {
			message = reason+" change context-free quality to " + contextFreeQuality + ". " + message;
		}
		this.contextFreeQuality = contextFreeQuality;
	}

	/**
	 * Perform context-free classification. Compute only once.
	 * - {@link #rawText}
	 * - {@link #heading}
	 * - {@link #linkDensity}
	 * - {@link #contextFreeQuality}
	 * The second invocation of this method won't do anything
	 */
	public void initRawInfo() {
		if (rawText != null) {
			return;
		}

		heading = false;
		linksLength = 0;

		StringBuilder sb = new StringBuilder();
		for (Node n : this) {
			if (n instanceof TextNode) {
				String nodeRawText = ((TextNode) n).text();
				sb.append(nodeRawText);

				if (NodeHelper.isLink(n))
					linksLength += nodeRawText.length();
			}
			/*
			if one of node in the fragment is heading, so the fragment is heading
			example: <h1><span>hello</span> world <img/></h1>
			 */
			if (!heading) {
				if (NodeHelper.isHeading(n)) {
					heading = true;
				}
			}
		}
		rawText = sb.toString();

		if (Configuration.DEBUG) {
			message = (message == null ? "" : message) + (isHeading() ? "Heading" : "");
		}

		/*compute stopwords density*/

		if (!Strings.isNullOrEmpty(conf.language())) {
			try {
				SortedSet<String> stopwords = StopwordsManager.getStopwords(conf.language());

				Iterable<String> words = Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().split(rawText);
				Iterator<String> it = words.iterator();
				this.countWords = 0;
				this.countStopwords = 0;
				while (it.hasNext()) {
					if (stopwords.contains(it.next())) {
						this.countStopwords++;
					}
					this.countWords++;
				}

				this.stopwordsDensity = 1.0*countStopwords / countWords;
				this.stopwordsChecking = true;
			} catch (Exception e) {
				message += "Failed load stopwords of "+conf.language()+": "+e.getMessage()+". ";
				this.stopwordsChecking = false;
				e.printStackTrace();
			}
		}

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
			case BAD:  color = isEven() ? "#FF8888": "#FF4444"; break;
			case GOOD: color= isEven() ? "#88FF88" : "#44FF44"; break;
			case SHORT: color= isEven() ? "##ffff88" : "#ffff44"; break;
			case NEAR_GOOD: color=isEven() ? "#88ffff" : "#40e0d0"; break;
		}

		for (int i=0; i<this.size(); i++) {
			Node n = this.get(i);
			String notice = HtmlEscapers.htmlEscaper().escape("[" + this.getId() + "." + i + "] " + this.message);
			try {
				n.wrap(String.format("<span style=\"background:%s\" title=\"%s\"></span>", color, notice));
			}
			catch (Exception ex) {
				System.err.println("Error Jsoup wrap on '" + notice + "'");
				ex.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		if (Configuration.DEBUG) {
			return "["+this.getId()+"] " + message;
		}

		return String.format("[%d] %s length=%d link=%.3g stopwords=%.3g", getId(), getQuality(), rawText.length(), getLinkDensity(), getStopwordsDensity());
	}


	public int getId() {
		return id;
	}

	private boolean isEven() {
		return id % 2 == 0;
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

