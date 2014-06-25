package dh.tool.justext;

import com.google.common.base.Strings;
import dh.tool.jsoup.NodeHelper;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.Iterator;
import java.util.LinkedList;

public class Extractor {
	private final Configuration conf;

	public Extractor() {
		this(Configuration.DEFAULT);
	}
	public Extractor(Configuration conf) {
		this.conf = conf;
	}

	public void removeBoilerplate(Document document) {
		removeBoilerplate(document, false);
	}

	public void removeBoilerplate(Document document, boolean colorize) {
		document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
		if (conf.isCleanUselessTag()) {
			cleanUselessContent(document);
		}

		LinkedList<Paragraph> paragraphs = conf.isProcessOnlyBody() ?
				computeParagraphs(document.body()) :
				computeParagraphs(document);

		new QualityComputation(paragraphs, conf).process();

		if (colorize) {
			for (Paragraph p : paragraphs) {
				p.colorizeParagraph();
			}
		}
		else {
			//remove all BAD node paragraphs
			for (Paragraph p : paragraphs) {
				if (p.getQuality()== Paragraph.Quality.BAD) {
					for (Node n : p) {
						n.remove();
					}
				}
			}

			//clean empty tags
			if (conf.isCleanEmptyTag()) {
				NodeHelper.cleanEmptyElements(document);
			}
		}
	}

	public static class QualityComputation {
		private LinkedList<Paragraph> paragraphs;
		private Configuration conf;

		public QualityComputation(LinkedList<Paragraph> paragraphs, Configuration conf) {
			this.paragraphs = paragraphs;
			this.conf = conf;
		}

		public void process() {
			//performs context-free classification

			for (Paragraph p : paragraphs) {
				p.initRawInfo();
			}

			//pre-process heading

			preProcessHeading();

			//context-sensitive classification

			processEdges(paragraphs.iterator()); //left edge
			processEdges(paragraphs.descendingIterator()); //right edge

			int left;
			for (int i=0; i<paragraphs.size();) {
				if (paragraphs.get(i).isNearOrShort()) {
					left = i-1;
					while (paragraphs.get(i).isNearOrShort() && i<paragraphs.size()) {
						i++;
					}
					processChunk(left, i);
				}
				i++;
			}

			//post-process heading

			postProcessHeading();
		}

		public void preProcessHeading() {
			if (!conf.isProcessHeadings()) {
				return;
			}
			//find all SHORT heading paragraph which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph shortHeading = paragraphs.get(i);
				if (shortHeading.isHeading() && shortHeading.getContextFreeQuality()==Paragraph.Quality.SHORT) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.getMaxHeadingDistance(); j++) {
						Paragraph p = paragraphs.get(j);
						if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
							shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading");
							break;
						}
						distanceToFirstGood += p.getLength();
					}
				}
			}
		}

		public void postProcessHeading() {
			if (!conf.isProcessHeadings()) {
				return;
			}

			//find all heading paragraph NON-BAD in Context-free which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph nonBadHeading = paragraphs.get(i);
				if (nonBadHeading.isHeading() && nonBadHeading.getContextFreeQuality()!=Paragraph.Quality.BAD) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.getMaxHeadingDistance(); j++) {
						Paragraph p = paragraphs.get(j);
						if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
							nonBadHeading.setQuality(Paragraph.Quality.GOOD, "Post-heading");
							break;
						}
						distanceToFirstGood += p.getLength();
					}
				}
			}

			if (conf.isRemoveTitle()) {
				//remove first good heading
				for (int i=0; i<paragraphs.size(); i++) {
					Paragraph p = paragraphs.get(i);
					if (p.isHeading() && p.getQuality()== Paragraph.Quality.GOOD) {
						p.setQuality(Paragraph.Quality.BAD, "RemoveTitle");
						return;
					}
				}
			}
		}

		private void processEdges(Iterator<Paragraph> it) {
			Paragraph p;
			while (it.hasNext() && (p = it.next()).isNearOrShort()) {
				if (conf.isRemoveEdgeContent()) {
					p.setQuality(Paragraph.Quality.BAD, "ProcessEdge"); // content from edge are often boilerplate
				} else {
					if (p.getQuality() == Paragraph.Quality.NEAR_GOOD) {
						p.setQuality(Paragraph.Quality.GOOD, "ProcessEdge"); //NEAR_GOOD becomes GOOD
					}
					else {
						p.setQuality(Paragraph.Quality.BAD, "ProcessEdge"); //SHORT becomes BAD
					}
				}
			}
		}

		private void processChunk(int leftPos, int rightPos) {
			Paragraph left = paragraphs.get(leftPos);
			Paragraph right = paragraphs.get(rightPos);

			if (sameQuality(leftPos, rightPos, left, right, Paragraph.Quality.GOOD)) {
				return;
			}
			if (sameQuality(leftPos, rightPos, left, right, Paragraph.Quality.BAD)) {
				return;
			}

			if (left.getQuality()==Paragraph.Quality.BAD) {
				/* B, S->B, N->G, ?->G, ?->G, G */

				int i;
				for (i=leftPos+1; i<rightPos; i++) {
					Paragraph p = paragraphs.get(i);
					if (p.getQuality()== Paragraph.Quality.NEAR_GOOD) //found the nearest NEAR_GOOD from the extremity BAD
						break;
					if (Configuration.DEBUG) {
						if (p.getQuality() != Paragraph.Quality.SHORT) {
							throw new IllegalStateException();
						}
					}
					p.setQuality(Paragraph.Quality.BAD, "Context-sensitive-dif");
				}

				for (int j=i; j<rightPos; j++) {
					paragraphs.get(j).setQuality(Paragraph.Quality.GOOD, "Context-sensitive-dif");
				}
				return;
			}

			if (right.getQuality()==Paragraph.Quality.BAD) {
				/* G, ?->G, ?->G, N->G, S->B, B */

				int i;
				for (i=rightPos-1; i>leftPos; i--) {
					Paragraph p = paragraphs.get(i);
					if (p.getQuality()== Paragraph.Quality.NEAR_GOOD) //found the nearest NEAR_GOOD from the extremity BAD
						break;
					if (Configuration.DEBUG) {
						if (p.getQuality() != Paragraph.Quality.SHORT) {
							throw new IllegalStateException();
						}
					}
					p.setQuality(Paragraph.Quality.BAD, "Context-sensitive-dif");
				}

				for (int j=i; j>leftPos; j--) {
					paragraphs.get(j).setQuality(Paragraph.Quality.GOOD, "Context-sensitive-dif");
				}
				return;
			}
		}

		private boolean sameQuality(int leftPos, int rightPos, Paragraph left, Paragraph right, Paragraph.Quality q) {
			if (left.getQuality()==q && right.getQuality()==q) {
				for (int i=leftPos+1; i<rightPos; i++) {
					paragraphs.get(i).setQuality(q, "Context-sensitive-eq");
				}
				return true;
			}
			return false;
		}
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

	private LinkedList<Paragraph> computeParagraphs(Node node) {
		ParagraphsExplorer pe = new ParagraphsExplorer(conf);
		node.traverse(pe);
		return pe.getParagraphs();
	}
}
