package dh.tool.justext.demo.event;

import dh.tool.justext.Configuration;
import org.jsoup.nodes.Document;

/**
 * Created by hiep on 28/06/2014.
 */
public class ExtractionRequest {
	public final Document document;
	public final Configuration configuration;

	public ExtractionRequest(Document document, Configuration configuration) {
		this.document = document;
		this.configuration = configuration;
	}
}
