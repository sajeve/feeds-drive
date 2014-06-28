package dh.tool.justext.demo;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import dh.tool.justext.Configuration;
import dh.tool.justext.Extractor;
import dh.tool.justext.demo.event.ExtractionRequest;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 28/06/2014.
 */
public class MainFrame extends JFrame {
	private static final Logger Log = LogManager.getLogger(MainFrame.class.getName());

	public MainFrame() throws HeadlessException {
		this.setTitle("JusText Demo");
		Container contentPane = this.getContentPane();
		this.setSize(new Dimension(800, 600));

		DesignGridLayout layout = new DesignGridLayout(this);
		buildGui(layout);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MainApp.EVENT_BUS.register(this);
	}

	private final ConfigPanel configPanel1 = new ConfigPanel();
	private final ConfigPanel configPanel2 = new ConfigPanel();
	private final ConfigPanel configPanel3 = new ConfigPanel();
	private final JTextField txtUrl = new JFormattedTextField();

	private final JProgressBar progressBar = new JProgressBar();

	private final WebBrowser webResult = new WebBrowser();
	private final WebBrowser webResultDecorated = new WebBrowser();
	private final WebBrowser webPreProcess = new WebBrowser();
	private final WebBrowser webOriginal = new WebBrowser();
	private final WebBrowser webResultIgnoreLang = new WebBrowser();
	private final WebBrowser webResultIgnoreLangDecorated = new WebBrowser();

	private void buildGui(DesignGridLayout layout) {
		JTabbedPane tabbedWebResult = new JTabbedPane();
		tabbedWebResult.addTab("Final", webResult);
		tabbedWebResult.addTab("Decoration", webResultDecorated);
		tabbedWebResult.addTab("Original", webOriginal);
		tabbedWebResult.addTab("Pre-Process", webPreProcess);
		tabbedWebResult.addTab("Ignore Lang", webResultIgnoreLang);
		tabbedWebResult.addTab("Ignore Lang Decoration ", webResultIgnoreLangDecorated);
		progressBar.setIndeterminate(true);
		progressBar.setVisible(false);
		layout.row().grid().add(txtUrl);
		layout.row().grid().add(configPanel1, configPanel2, configPanel3);
		layout.row().grid().add(progressBar, 3);
		layout.row().grid().add(tabbedWebResult);
	}

	@Subscribe
	public void doEventExtraction(final Configuration config) {
		Log.debug(MainApp.EventBusMarker, "doEventExtraction", config);
		final String address = txtUrl.getText();

		SwingWorker worker = new SwingWorker() {
			@Override
			protected Object doInBackground() throws Exception {
				try {
					Stopwatch sw = Stopwatch.createStarted();
					Document document = Jsoup.parse(new URL(address), 60*1000);
					sw.stop();
					Log.info(String.format("Download and parse: %d ms - '%s'", sw.elapsed(TimeUnit.MILLISECONDS), address));
					MainApp.EVENT_BUS.post(new ExtractionRequest(document, config));
				}
				catch (Exception ex) {
					Log.error("Failed", ex);
				}
				return null;
			}
//			@Override
//			protected void done() {
//				super.done();
//			}
		};

		worker.execute();
	}

	@Subscribe
	public void extractionStart(final Document document) {
		Log.debug(MainApp.EventBusMarker, "extractionStart");

	}

	@Subscribe
	public void extractionEnd(final Document document) {
		Log.debug(MainApp.EventBusMarker, "extractionEnd");

	}
}
