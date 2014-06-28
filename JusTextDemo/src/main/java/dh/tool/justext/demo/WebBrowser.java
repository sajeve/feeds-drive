package dh.tool.justext.demo;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import com.google.common.eventbus.Subscribe;
import dh.tool.justext.Extractor;
import dh.tool.justext.demo.event.ExtractionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jsoup.nodes.Document;
import sun.applet.Main;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hiep on 28/06/2014.
 */
public class WebBrowser extends JPanel {
	private static final Logger Log = LogManager.getLogger(WebBrowser.class.getName());
	public WebBrowser() {
		initGui();
	}

	private final JWebBrowser webBrowser = new JWebBrowser();
	private final RSyntaxTextArea txaSource = new RSyntaxTextArea();
	private final JTextField txtStatus = new JTextField();

	public void initGui() {
		webBrowser.setBarsVisible(false);
		webBrowser.setStatusBarVisible(true);

		RTextScrollPane txsSource = new RTextScrollPane(txaSource);

		JTabbedPane contentPane = new JTabbedPane();
		contentPane.setTabPlacement(JTabbedPane.BOTTOM);
		contentPane.addTab("Web", webBrowser);
		contentPane.addTab("Source", txsSource);

		txtStatus.setEditable(false);

		this.setLayout(new BorderLayout());
		this.add(contentPane, BorderLayout.CENTER);
		this.add(txtStatus, BorderLayout.PAGE_END);

		MainApp.EVENT_BUS.register(this);
	}

	@Subscribe public void onReceiveExtractionRequest(ExtractionRequest request) {
		try {
			Log.info("onReceiveExtractionRequest");
			final Document doc = request.document.clone();
			new Extractor(request.configuration).removeBoilerplate(doc);
			//display doc:
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						webBrowser.setHTMLContent(doc.outerHtml());
					}
					catch (Exception ex) {
						Log.error("Failed", ex);
					}
				}
			});
		}
		catch (Exception ex) {
			Log.error("Failed", ex);
		}
	}
}
