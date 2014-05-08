package dh.newspaper.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import dh.newspaper.parser.ContentParser;
import junit.framework.TestCase;

public class ContentParserTest extends TestCase {

	ContentParser contentParser;

	@Override
	protected void setUp() throws Exception {
		contentParser = new ContentParser();
	}

	public void extractVnexpressTest() throws IOException {
		String address = "http://vnexpress.net/tin-tuc/thoi-su/cau-rong-nhan-giai-ky-thuat-xuat-sac-quoc-te-2985651.html";
		String vnexpressContentSelector = "div.short_intro, div.relative_new, div.fck_detail";
		String content = contentParser.extractContent(address, vnexpressContentSelector).html();
		assertTrue(content.length() > 100);
		System.out.println(content);
		writeToFile("vnexpress.sample.html",content,true);
	}

	private static void writeToFile(String filename, String content, boolean wrapHtml) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
		if (wrapHtml) {
			writer.write("<!DOCTYPE html>\n");
			writer.write("<html><head><meta charset=\"utf-8\"></head><body>");
		}
		writer.write(content);
		if (wrapHtml) {
			writer.write("</body></html>");
		}
		writer.close();
	}

	public static void testBasicParseString() {
		String source = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>xướng tên</body></html>";
		Document doc = Jsoup.parse(source);
		doc.outputSettings().escapeMode(EscapeMode.xhtml);
		System.out.println(doc.toString());
	}
	public static void testBasicParseFile() throws IOException {
		String source = "vnexpress.source.html";
		Document doc = Jsoup.parse(new File(source), "UTF-8");
		assertNotNull(doc);
		System.out.println(doc.toString());
	}
}
