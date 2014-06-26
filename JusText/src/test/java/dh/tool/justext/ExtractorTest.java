package dh.tool.justext;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import dh.tool.TestUtils;
import dh.tool.jsoup.NodeHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 26/06/2014.
 */
public class ExtractorTest {
	@Test
	public void testHuffing() throws IOException, CloneNotSupportedException {
		Configuration globalConf = new Configuration.Builder().lengthLow(50).maxLinkDensity(0.25).build();
		//Configuration globalConf = Configuration.DEFAULT;
		{
			Configuration conf = new Configuration.Builder(globalConf).language("vn").build();
			executeTestCase("http://vietnamnet.vn/vn/chinh-tri/182839/tq-ngang-nhien-phat-hanh-ban-do--nuot-chung--bien-dong.html", "vietnamnet1", conf);
		}
		{
			Configuration conf = new Configuration.Builder(globalConf).build();
			executeTestCase("http://www.huffingtonpost.com/2014/06/25/googles-massive-plan-to-t_n_5530653.html", "huffing1", conf);
		}
		{
			Configuration conf = new Configuration.Builder(globalConf).language("vn").build();
			executeTestCase("http://ngoisao.net/tin-tuc/phong-cach/lam-dep/10-sao-viet-thua-nhan-phau-thuat-tham-my-3009242.html", "ngoisao1", conf);
		}
		{
			Configuration conf = new Configuration.Builder(globalConf).language("vn").build();
			executeTestCase("http://mobile.nytimes.com/2014/06/26/world/africa/buffeted-by-tumult-jewish-population-in-tunisia-dwindles.html", "nytimes1", conf);
		}
	}

	private void executeTestCase(String url, String fileName, Configuration conf) throws IOException, CloneNotSupportedException {
		Stopwatch sw = Stopwatch.createStarted();
		Document originDoc = Jsoup.parse(new URL(url), Integer.MAX_VALUE);
		System.out.println("Download and parse "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");
		TestUtils.writeToFile(fileName + "-origin.html", originDoc.html(), false);
		{
			Document document = originDoc.clone();
			sw.reset().start();
			Extractor.cleanUselessContent(document);
			System.out.println("Clean ignorable content " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
			TestUtils.writeToFile(fileName+"-cleanIgnorableTags.html", originDoc.html(), false);
		}
		Extractor extractor = new Extractor(conf);
		{
			Document document = originDoc.clone();
			sw.reset().start();
			extractor.removeBoilerplate(document);
			System.out.println("first remove boilerplate (load language) " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
		}
		{
			Document document = originDoc.clone();
			sw.reset().start();
			extractor.removeBoilerplate(document);
			System.out.println("remove boilerplate " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
			TestUtils.writeToFile(fileName + "-final.html", document.html(), false);
		}
		{
			Document document = originDoc.clone();
			sw.reset().start();
			extractor.decorateBoilerplate(document);
			System.out.println("decorate boilerplate " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
			TestUtils.writeToFile(fileName + "-decorated.html", document.html(), false);
		}

		if (!Strings.isNullOrEmpty(conf.language())) {
			Document document = originDoc.clone();
			Configuration confNoLang = new Configuration.Builder(conf).language(null).build();
			Extractor extractorNoLang = new Extractor(confNoLang);
			sw.reset().start();
			extractorNoLang.decorateBoilerplate(document);
			System.out.println("decorate boilerplate (nolang)" + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");
			TestUtils.writeToFile(fileName + "-decorated-nolang.html", document.html(), false);
		}
	}

	@Test
	public void testFreeContextClassify() throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		Document document = Jsoup.parse(new URL("http://dantri.com.vn/phap-luat/phat-hien-chan-dong-hon-14000-so-dien-thoai-bi-nghe-len-theo-doi-891664.htm"), Integer.MAX_VALUE);
		System.out.println("Download and parse "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		TestUtils.writeToFile("vnexpress1-origin.html", document.html(), false);

		sw.reset().start();

		Configuration conf = new Configuration.Builder()
				.language("vn")
				.removeTitle(true)
				.build();
		Extractor extractor = new Extractor(conf);
		extractor.decorateBoilerplate(document);

		System.out.println("Remove boilerplate " + sw.elapsed(TimeUnit.MILLISECONDS) + " ms");

		TestUtils.writeToFile("vnexpress1-final.html", document.html(), false);
	}

	@Test
	public void testFreeContextClassify2() throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		Document document = Jsoup.parse(new URL("http://www.huffingtonpost.fr/2014/06/24/italie-uruguay-but-morsure-huitiemes-de-finale_n_5526485.html?utm_hp_ref=france"), Integer.MAX_VALUE);
		System.out.println("Download and parse "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		TestUtils.writeToFile("nyt-origin.html", document.html(), false);

//		Extractor.cleanUselessContent(document);
//		TestUtils.writeToFile("nyt-clean.html", document.html(), false);

		sw.reset().start();

		Configuration conf = new Configuration.Builder()
				//.language("fr") //no need, language is auto-detected
				.build();
		Extractor extractor = new Extractor(conf);
		extractor.decorateBoilerplate(document);

		System.out.println("decorate boilerplate "+sw.elapsed(TimeUnit.MILLISECONDS)+" ms");

		TestUtils.writeToFile("nyt-final.html", document.html(), false);
	}

	@Test
	public void testCleanUselessContent() {
		Document document = Jsoup.parse("<div><textarea class=\"hp-slideshow-share-url\" rows=\"3\" cols=\"70\" spellcheck=\"false\"></textarea></div>");

		Element e = document.select("textarea").first();

		Assert.assertTrue(NodeHelper.isIgnorableTag(e));

		System.out.println(document);

		System.out.println("Cleaning..");
		Extractor.cleanUselessContent(document);

		System.out.println(document);
	}
}
