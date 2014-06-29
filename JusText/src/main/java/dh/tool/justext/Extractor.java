package dh.tool.justext;

import com.google.common.base.Strings;
import dh.tool.common.ICancellation;
import dh.tool.jsoup.NodeHelper;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CancellationException;

public class Extractor {
	private Configuration conf;
	private final ICancellation cancellation;

	public Extractor() {
		this(Configuration.DEFAULT, null);
	}
	public Extractor(Configuration conf) {
		this(conf, null);
	}
	public Extractor(Configuration conf, ICancellation cancellation) {
		this.conf = conf;
		this.cancellation = cancellation;
	}

	/**
	 * The document will be at dirty state if cancel happen
	 * throws CancellationException
	 */
	public void removeBoilerplate(Document document) {
		process(document, false);
	}
	/**
	 * The document will be at dirty state if cancel happen
	 * throws CancellationException
	 */
	public void decorateBoilerplate(Document document) {
		process(document, true);
	}

	/**
	 * throws CancellationException
	 */
	private void process(Document document, boolean colorize) {
		checkCancellation();

		document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

		String lang = null;
		if (conf.autoDetectLanguage() && Strings.isNullOrEmpty(conf.language())) {
			//auto detect language only if user did not configured language
			lang = NodeHelper.detectLanguage(document);

			if (!Strings.isNullOrEmpty(lang)) {
				//found language, check if we have stop words list on this language
				lang = StopwordsManager.getLanguage(lang.toLowerCase());
				if (!Strings.isNullOrEmpty(lang)) {
					/*
					client did not configured the language but we detected the language of the page and we have stop words list on it
					so we will exceptionally change the language configuration by cloning the actual config that the client gave us
					*/
					conf = (new Configuration.Builder(conf)).language(lang).build();
				}
			}
		}

		checkCancellation();

		if (conf.preCleanUselessContent()) {
			cleanUselessContent(document);
		}

		checkCancellation();

		LinkedList<Paragraph> paragraphs = conf.processOnlyBody() ?
				computeParagraphs(document.body()) :
				computeParagraphs(document);

		checkCancellation();

		new QualityComputation(paragraphs, conf, cancellation).process();

		checkCancellation();

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

			checkCancellation();

			//clean empty tags
			if (conf.postCleanBoilerplateTags()) {
				NodeHelper.cleanEmptyElements(document);
				NodeHelper.unwrapRedundancyTags(document);
			}
		}
	}

	private void checkCancellation() {
		if (cancellation!=null && cancellation.isCancelled()) {
			throw new CancellationException();
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
				if (node instanceof Element) {
					Iterator<Attribute> it = node.attributes().iterator();
					while (it.hasNext()) {
						Attribute a = it.next();
						if (NodeHelper.isIgnorableAttribute(a.getKey())) {
							node.removeAttr(a.getKey());
						}
					}
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

	private static class QualityComputation {
		private final LinkedList<Paragraph> paragraphs;
		private final Configuration conf;
		private final ICancellation cancellation;

		public QualityComputation(LinkedList<Paragraph> paragraphs, Configuration conf, ICancellation cancellation) {
			this.paragraphs = paragraphs;
			this.conf = conf;
			this.cancellation = cancellation;
		}

		public void process() {
			//performs context-free classification

			for (Paragraph p : paragraphs) {
				checkCancellation();
				p.initRawInfo();
			}

			//pre-process heading

			checkCancellation();

			if (conf.contentAlwaysHasTitle()) {
				preProcessHeading2();
			}
			else {
				preProcessHeading();
			}

			//context-sensitive classification

			processEdges(paragraphs.iterator()); //top edge
			processEdges(paragraphs.descendingIterator()); //bottom edge

			int left;
			for (int i=0; i<paragraphs.size();) {
				checkCancellation();
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

			if (conf.contentAlwaysHasTitle()) {
				findTitle();
			}

			if (conf.removeTitle()) {
				//remove first good heading
				for (int i=0; i<paragraphs.size(); i++) {
					checkCancellation();

					Paragraph p = paragraphs.get(i);
					if (p.isHeading() && p.getQuality()== Paragraph.Quality.GOOD) {
						p.setQuality(Paragraph.Quality.BAD, "RemoveTitle");
						return;
					}
				}
			}
		}

		/**
		 * jusText algorithm find all SHORT heading paragraph which is not too far away from the first GOOD paragraph
		 * promote these heading from SHORT to NEAR_GOOD
		 */
		public void preProcessHeading() {
			if (!conf.processHeadings()) {
				return;
			}
			//find all SHORT heading paragraph which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph shortHeading = paragraphs.get(i);
				if (shortHeading.isHeading() && shortHeading.getContextFreeQuality()==Paragraph.Quality.SHORT) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.maxHeadingDistance(); j++) {
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

		/**
		 * enhance by Hiep after trying: http://www.huffingtonpost.com/2014/06/25/googles-massive-plan-to-t_n_5530653.html
		 * give more tolerance for NEAR-GOOD text after a heading paragraph.
		 *
		 * After heading, we often have a small resume paragraph (excerpt) which is sometimes NEAR_GOOD
		 * In this case, we will force it to GOOD (context-free) and promote SHORT heading to NEAR_GOOD
		 */
		public void preProcessHeading2() {
			if (!conf.processHeadings()) {
				return;
			}
			//find all SHORT heading paragraph which is not too far away from the first GOOD or NEAR_GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph shortHeading = paragraphs.get(i);
				if (shortHeading.isHeading() && shortHeading.getContextFreeQuality()==Paragraph.Quality.SHORT) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.maxHeadingDistance(); j++) {
						Paragraph p = paragraphs.get(j);
						if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
							//a SHORT heading near a GOOD paragraph: normal jusText processing
							shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading");
							break;
						}
						if (p.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
							//a SHORT heading near a NEAR_GOOD paragraph: excerpt detected
							shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading-tolerance");
							p.setContextFreeQuality(Paragraph.Quality.GOOD, "Pre-heading-excerpt");
						}
						distanceToFirstGood += p.getLength();
					}
				}
			}
		}

		/**
		 * make sure that we has a title in article
		 * if we did not have any heading GOOD paragraph, so
		 * we will find the nearest context-free NEAR_GOOD heading before the first GOOD paragraph.
		 * We will use this paragraph as Title, so promote it to GOOD.
		 */
		public void findTitle() {
			checkCancellation();

			for (Paragraph p : paragraphs) {
				if (p.isHeading() && p.getQuality() == Paragraph.Quality.GOOD) {
					//we already has a GOOD heading paragraph, nothing to do here
					return;
				}
			}

			//find the nearest NEAR_GOOD heading before the first GOOD paragraph
			Paragraph lastNearGoodHeading = null;
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph p = paragraphs.get(i);
				if (p.isHeading() && p.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
					lastNearGoodHeading = p;
				}
				if (p.getQuality() == Paragraph.Quality.GOOD) {
					break;
				}
			}

			//promote it to GOOD (so we will have title)
			if (lastNearGoodHeading != null) {
				lastNearGoodHeading.setQuality(Paragraph.Quality.GOOD, "FindTitle");
			}
		}

		public void postProcessHeading() {
			checkCancellation();

			if (!conf.processHeadings()) {
				return;
			}

			//find all heading paragraph NON-BAD in Context-free which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph nonBadHeading = paragraphs.get(i);
				if (nonBadHeading.isHeading()) {
					if (nonBadHeading.getQuality() == Paragraph.Quality.GOOD) {
						//the title is already here, no need to search further
						return;
					}
					if (nonBadHeading.getContextFreeQuality()!=Paragraph.Quality.BAD) {
						int distanceToFirstGood = 0;
						for (int j = i + 1; j < paragraphs.size() && distanceToFirstGood < conf.maxHeadingDistance(); j++) {
							Paragraph p = paragraphs.get(j);
							if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
								nonBadHeading.setQuality(Paragraph.Quality.GOOD, "Post-heading");
								break;
							}
							distanceToFirstGood += p.getLength();
						}
					}
				}
			}
		}

		private void processEdges(Iterator<Paragraph> it) {
			checkCancellation();
			Paragraph p;
			while (it.hasNext() && (p = it.next()).isNearOrShort()) {
				if (conf.strictOnEdgeContent()) {
					p.setQuality(Paragraph.Quality.BAD, "ProcessEdge"); // strict way: content from edge are often boilerplate
				} else {
					//tolerate NEAR_GOOD content on edge, promote it to GOOD
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

		private void checkCancellation() {
			if (cancellation!=null && cancellation.isCancelled()) {
				throw new CancellationException();
			}
		}
	}
}
