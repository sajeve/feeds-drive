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
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36";

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
					Log.error("Failed", e);
				}
			}
		});

		NativeInterface.runEventPump();
	}
}
