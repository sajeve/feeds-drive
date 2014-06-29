package dh.tool.justext.demo;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import com.google.common.eventbus.Subscribe;
import dh.tool.justext.Configuration;
import dh.tool.justext.demo.common.ExtractionReply;
import dh.tool.justext.demo.common.ExtractionRequest;
import dh.tool.swing.CodeEditor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hiep on 28/06/2014.
 */
public abstract class WebBrowser extends JPanel {
	private static final Logger Log = LogManager.getLogger(WebBrowser.class.getName());
	private final JWebBrowser webBrowser = new JWebBrowser();
	private final CodeEditor sourceEditor = new CodeEditor(10, 80, SyntaxConstants.SYNTAX_STYLE_HTML);
	private final JTextArea statusMessage = new JTextArea(2, 80);

	private Document document_;
	private Configuration config_;

	public WebBrowser() {
		webBrowser.setBarsVisible(false);
		webBrowser.setStatusBarVisible(true);

		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setTabPlacement(JTabbedPane.BOTTOM);
		contentPane.addTab("Web", webBrowser);
		contentPane.addTab("Source", sourceEditor);

		statusMessage.setEditable(false);

		this.setLayout(new BorderLayout());
		this.add(contentPane, BorderLayout.CENTER);
		this.add(statusMessage, BorderLayout.PAGE_END);

		MainApp.EVENT_BUS.register(this);
	}

	@Subscribe public void onReceiveExtractionRequest(final ExtractionRequest request) {
		try {
			Log.debug(MainApp.EventBusMarker, "onReceiveExtractionRequest");

			if (request.configuration != null) {
				config_ = request.configuration;
			}
			if (config_ == null) {
				config_ = Configuration.DEFAULT;
			}

			statusMessage.setText("Received extraction request "+request.document.baseUri());

			new SwingWorker<ExtractionReply, Void>() {
				@Override
				protected ExtractionReply doInBackground() throws Exception {
					//process request (create a clone to protect the original document_)
					return extract(request.document.clone(), config_);
				}

				@Override
				protected void done() {
					try {
						ExtractionReply extractionReply = get();
						document_ = extractionReply.getResult();
						String html = document_.outerHtml();
						webBrowser.setHTMLContent(html);
						sourceEditor.setText(html);
						statusMessage.setText(extractionReply.getStatusMessage());
					}
					catch (Exception ex) {
						Log.error("Failed display ExtractionReply", ex);
						statusMessage.setText(ex.getMessage());
					}
				}
			}.execute();
		}
		catch (Exception ex) {
			Log.error("Failed onReceiveExtractionRequest", ex);
		}
	}

	/**
	 * Process the request
	 */
	public abstract ExtractionReply extract(Document doc, Configuration conf);
}
