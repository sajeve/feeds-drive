package dh.tool.justext.demo.common;


import org.jsoup.nodes.Document;

/**
 * Created by hiep on 29/06/2014.
 */
public class ExtractionReply {
	private String address;
	private long timeToParse;
	private String errorMessage;
	private Document result;
	private boolean success;

	public ExtractionReply(String address, long timeToParse, Document result) {
		this.address = address;
		this.timeToParse = timeToParse;
		this.result = result;
		this.success = true;
	}

	public ExtractionReply(String address, long timeToParse, String errorMessage) {
		this.address = address;
		this.timeToParse = timeToParse;
		this.errorMessage = errorMessage;
		this.success = false;
	}

	public String getAddress() {
		return address;
	}

	public long getTimeToParse() {
		return timeToParse;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Document getResult() {
		return result;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getStatusMessage() {
		if (isSuccess()) {
			return String.format("%d ms - SUCCESS extracting %s", timeToParse, address);
		}
		else {
			return String.format("%d ms - FAILED extracting %s \n%s", timeToParse, address, errorMessage);
		}

	}
}
