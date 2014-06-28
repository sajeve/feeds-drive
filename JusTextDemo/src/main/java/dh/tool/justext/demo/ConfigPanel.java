package dh.tool.justext.demo;

import dh.tool.justext.Configuration;
import dh.tool.justext.Extractor;
import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by hiep on 28/06/2014.
 */
public class ConfigPanel extends JPanel {
	private static final Logger Log = LogManager.getLogger(ConfigPanel.class.getName());
	private final RSyntaxTextArea txaConfig = new RSyntaxTextArea(10,10);
	private final JButton btnApply = new JButton("Apply");
	private final JButton btnReset = new JButton("Reset");

	public ConfigPanel() {
		initGui();
		initEvents();
	}

	private void initGui() {
		DesignGridLayout layout = new DesignGridLayout(this);

		txaConfig.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		txaConfig.setCodeFoldingEnabled(true);
		RTextScrollPane txsConfig = new RTextScrollPane(txaConfig);

		layout.row().grid().add(txsConfig);
		layout.row().center().add(btnApply, btnReset);
	}

	private void initEvents() {
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Log.debug(MainApp.EventBusMarker, "Apply clicked");
				MainApp.EVENT_BUS.post(Configuration.DEFAULT);
			}
		});
	}
}
