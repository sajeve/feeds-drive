package dh.tool.justext.demo;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * Created by hiep on 28/06/2014.
 */
public class WebBrowser extends JPanel {

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
	}
}
