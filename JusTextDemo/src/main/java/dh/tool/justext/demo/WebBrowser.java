package dh.tool.justext.demo;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import com.google.common.eventbus.Subscribe;
import dh.tool.justext.Configuration;
import dh.tool.justext.demo.common.ExtractionReply;
import dh.tool.justext.demo.common.ExtractionRequest;
import dh.tool.swing.CodeEditor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by hiep on 28/06/2014.
 */
public abstract class WebBrowser extends JPanel {
	private static final Logger Log = LogManager.getLogger(WebBrowser.class.getName());
	private final JWebBrowser webBrowser = new JWebBrowser();
	private final CodeEditor sourceEditor = new CodeEditor(10, 80, SyntaxConstants.SYNTAX_STYLE_HTML);
	private final JTextArea statusMessage = new JTextArea(2, 80);
	private final JCheckBox disableCheckbox = new JCheckBox("Disable");

	private Document document_;
	private Configuration config_;

	public WebBrowser() {
		webBrowser.setBarsVisible(false);
		webBrowser.setStatusBarVisible(false);

		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setTabPlacement(JTabbedPane.BOTTOM);
		contentPane.addTab("Web", webBrowser);
		contentPane.addTab("Source", sourceEditor);

		statusMessage.setEditable(false);

		disableCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				try {
					webBrowser.setEnabled(!disableCheckbox.isSelected());
					sourceEditor.setEnabled(!disableCheckbox.isSelected());
				}
				catch (Exception ex) {
					Log.error("Disabling failed",ex);
					JOptionPane.showMessageDialog(WebBrowser.this, ex.toString(), "Disabling failed", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		this.setLayout(new BorderLayout());
		this.add(disableCheckbox, BorderLayout.PAGE_START);
		this.add(contentPane, BorderLayout.CENTER);
		this.add(statusMessage, BorderLayout.PAGE_END);

		MainApp.EVENT_BUS.register(this);
	}

	@Subscribe public void onReceiveExtractionRequest(final ExtractionRequest request) {
		try {
			if (disableCheckbox.isSelected()) return;

			Log.debug(MainApp.EventBusMarker, "onReceiveExtractionRequest");

			if (request.configuration != null) {
				config_ = request.configuration;
			}
			if (config_ == null) {
				config_ = Configuration.DEFAULT;
			}

			statusMessage.setText("Received extraction request "+request.document.baseUri());

			new SwingWorker<ExtractionReply, Void>() {
				private volatile Exception err;

				@Override
				protected ExtractionReply doInBackground() throws Exception {
					try {
						//process request (create a clone to protect the original document_)
						return extract(request.document.clone(), config_);
					}
					catch (Exception ex) {
						err = ex;
					}
					return null;
				}

				@Override
				protected void done() {
					try {
						if (err != null) throw err;

						ExtractionReply extractionReply = get();

						String html;
						if (extractionReply.getError() == null) {
							document_ = extractionReply.getResult();
							html = document_ != null ? document_.outerHtml() : extractionReply.getResultText();
						}
						else {
							html = "<pre>"+ExceptionUtils.getFullStackTrace(extractionReply.getError())+"</pre>";
						}

						if (html==null) {
							html="";
						}
						webBrowser.setHTMLContent(html);
						sourceEditor.setText(html);
						statusMessage.setText(extractionReply.getStatusMessage());
					} catch (Exception ex) {
						Log.error("Failed display ExtractionReply", ex);
						statusMessage.setText(ex.toString());
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
