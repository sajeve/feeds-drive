package dh.tool.justext.demo;

import dh.tool.justext.Configuration;
import dh.tool.swing.CodeEditor;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by hiep on 28/06/2014.
 */
public class ConfigPanel extends JPanel {
	private static final Logger Log = LogManager.getLogger(ConfigPanel.class.getName());
	private final CodeEditor configEditor = new CodeEditor(10, 10, SyntaxConstants.SYNTAX_STYLE_JAVA);
	private final JButton btnApply = new JButton("Apply");
	private final JButton btnReset = new JButton("Reset");

	public ConfigPanel() {
		initGui();
		initEvents();
	}

	private void initGui() {
		DesignGridLayout layout = new DesignGridLayout(this);
		layout.row().grid().add(configEditor);
		layout.row().center().add(btnApply, btnReset);
	}

	private void initEvents() {
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Log.debug(MainApp.EventBusMarker, "Apply clicked");
					Configuration config = new Configuration.Builder(configEditor.getText()).build();
					MainApp.EVENT_BUS.post(config);
				}
				catch (Exception ex) {
					Log.error("Failed Apply clicked", ex);
					JOptionPane.showMessageDialog(ConfigPanel.this, "Failed apply config: "+ex.getMessage(), "Failed apply config", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String defaultConfigStr = Configuration.DEFAULT.toString().replace(";", ";\n");
					configEditor.setText(defaultConfigStr);
				}
				catch (Exception ex) {
					Log.error("Failed Reset clicked", ex);
					JOptionPane.showMessageDialog(ConfigPanel.this, ex.getMessage());
				}
			}
		});
	}
}
