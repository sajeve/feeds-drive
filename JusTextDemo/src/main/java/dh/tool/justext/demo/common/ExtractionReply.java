package dh.tool.justext.demo.common;


import org.jsoup.nodes.Document;

/**
 * Created by hiep on 29/06/2014.
 */
public class ExtractionReply {
	private String address;
	private long timeToParse;
	private Exception error;
	private Document result;
	private String resultText;
	private boolean success;

	public ExtractionReply(String address, long timeToParse, Document result) {
		this.address = address;
		this.timeToParse = timeToParse;
		this.result = result;
		this.success = true;
	}

	public ExtractionReply(String address, long timeToParse, String cleanedText) {
		this.address = address;
		this.timeToParse = timeToParse;
		this.resultText = cleanedText;
		this.success = true;
	}

	public ExtractionReply(String address, long timeToParse, Exception ex) {
		this.address = address;
		this.timeToParse = timeToParse;
		this.error = ex;
		this.success = false;
	}

	public String getAddress() {
		return address;
	}

	public long getTimeToParse() {
		return timeToParse;
	}

	public Exception getError() {
		return error;
	}

	public Document getResult() {
		return result;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getResultText() {
		return resultText;
	}

	public String getStatusMessage() {
		if (isSuccess()) {
			return String.format("%d ms - SUCCESS extracting %s", timeToParse, address);
		}
		else {
			return String.format("%d ms - FAILED extracting %s \n%s", timeToParse, address, error.toString());
		}

	}
}
