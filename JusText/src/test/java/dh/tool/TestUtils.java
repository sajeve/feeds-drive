package dh.tool;

import java.io.*;

/**
 * Created by hiep on 8/05/2014.
 */
public class TestUtils {
	private static final String TAG = TestUtils.class.getName();

	public static void writeToFile(String filename, String content, boolean wrapHtml) throws IOException {
		System.out.println("Start write String to file " + filename);

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
		System.out.println("Write String to file OK " + filename);
	}

	public static void writeToFile(String filename, InputStream content, boolean wrapHtml) throws IOException {
		System.out.println("Start write InputStream to file " + filename);

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filename)));
		if (wrapHtml) {
			writer.write("<!DOCTYPE html>\n");
			writer.write("<html><head><meta charset=\"utf-8\"></head><body>");
		}

		BufferedInputStream bis = new BufferedInputStream(content);
		InputStreamReader sr = new InputStreamReader(bis);

		char[] buffer = new char[1024];
		while (sr.read(buffer) > 0) {
			writer.write(buffer);
		}

		if (wrapHtml) {
			writer.write("</body></html>");
		}
		writer.close();
		System.out.println("Write InputStream to file OK " + filename);
	}
}
