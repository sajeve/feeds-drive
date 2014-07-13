package dh.tool.justext.demo;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import com.google.common.eventbus.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.swing.*;

public class MainApp {
	private static final Logger Log = LogManager.getLogger(MainApp.class.getName());
	public static final Marker EventBusMarker = MarkerManager.getMarker("EventBus");

	public static final EventBus EVENT_BUS = new EventBus("GlobalEventBus");
	private MainFrame frmMain_ = new MainFrame();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		Log.info("Starting application..");
		UIUtils.setPreferredLookAndFeel();
		NativeInterface.open();
		// Here goes the rest of the program initialization

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					MainApp window = new MainApp();
					window.frmMain_.setVisible(true);
					Log.info("Application Started");
				} catch (Exception e) {
					Log.error("Failed start application", e);
				}
			}
		});

		NativeInterface.runEventPump();
	}
}
