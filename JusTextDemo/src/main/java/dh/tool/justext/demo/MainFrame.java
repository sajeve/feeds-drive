package dh.tool.justext.demo;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.Subscribe;
import dh.tool.justext.Configuration;
import dh.tool.justext.Extractor;
import dh.tool.justext.demo.common.ExtractionReply;
import dh.tool.justext.demo.common.ExtractionRequest;
import net.java.dev.designgridlayout.DesignGridLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 28/06/2014.
 */
public class MainFrame extends JFrame {
	private static final Logger Log = LogManager.getLogger(MainFrame.class.getName());

	public MainFrame() throws HeadlessException {
		this.setTitle("JusText Demo");
		this.setSize(new Dimension(800, 600));

		initGui();

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		MainApp.EVENT_BUS.register(this);
	}

	private final JComboBox comboAddress = new JComboBox();
	private final JButton buttonGo = new JButton("Go");
	private final ConfigPanel configPanel1 = new ConfigPanel();
	private final ConfigPanel configPanel2 = new ConfigPanel();
	private final ConfigPanel configPanel3 = new ConfigPanel();

	private Document document_;

	private final WebBrowser webResult = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			Stopwatch sw = Stopwatch.createStarted();
			try {
				new Extractor(conf).removeBoilerplate(doc);
				sw.stop();
				Log.info(String.format("removeBoilerplate - %d ms: %s", sw.elapsed(TimeUnit.MILLISECONDS), doc.baseUri()));
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), doc);
			} catch (Exception ex) {
				sw.stop();
				Log.error("Failed", ex);
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), ex.getMessage());
			}
		}
	};
	private final WebBrowser webResultDecorated = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			Stopwatch sw = Stopwatch.createStarted();
			try {
				new Extractor(conf).decorateBoilerplate(doc);
				sw.stop();
				Log.info(String.format("decorateBoilerplate - %d ms: %s", sw.elapsed(TimeUnit.MILLISECONDS), doc.baseUri()));
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), doc);
			} catch (Exception ex) {
				sw.stop();
				Log.error("Failed", ex);
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), ex.getMessage());
			}
		}
	};
	private final WebBrowser webPreProcess = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			Stopwatch sw = Stopwatch.createStarted();
			try {
				Extractor.cleanUselessContent(doc);
				sw.stop();
				Log.info(String.format("cleanUselessContent - %d ms: %s", sw.elapsed(TimeUnit.MILLISECONDS), doc.baseUri()));
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), doc);
			} catch (Exception ex) {
				sw.stop();
				Log.error("Failed", ex);
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), ex.getMessage());
			}
		}
	};
	private final WebBrowser webOriginal = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			return new ExtractionReply(doc.baseUri(), 0, doc);
		}
	};
	private final WebBrowser webResultIgnoreLang = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			Configuration confIgnoresLang = (new Configuration.Builder(conf)).autoDetectLanguage(false).language(null).build();
			Stopwatch sw = Stopwatch.createStarted();
			try {
				new Extractor(confIgnoresLang).removeBoilerplate(doc);
				sw.stop();
				Log.info(String.format("removeBoilerplate NoLang - %d ms: %s", sw.elapsed(TimeUnit.MILLISECONDS), doc.baseUri()));
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), doc);
			} catch (Exception ex) {
				sw.stop();
				Log.error("Failed", ex);
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), ex.getMessage());
			}
		}
	};
	private final WebBrowser webResultIgnoreLangDecorated = new WebBrowser() {
		@Override
		public ExtractionReply extract(Document doc, Configuration conf) {
			Configuration confIgnoresLang = (new Configuration.Builder(conf)).autoDetectLanguage(false).language(null).build();
			Stopwatch sw = Stopwatch.createStarted();
			try {
				new Extractor(confIgnoresLang).decorateBoilerplate(doc);
				sw.stop();
				Log.info(String.format("decorateBoilerplate NoLang - %d ms: %s", sw.elapsed(TimeUnit.MILLISECONDS), doc.baseUri()));
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), doc);
			} catch (Exception ex) {
				sw.stop();
				Log.error("Failed", ex);
				return new ExtractionReply(doc.baseUri(), sw.elapsed(TimeUnit.MILLISECONDS), ex.getMessage());
			}
		}
	};

	private final ActionListener loadAddress = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			final String address = comboAddress.getSelectedItem().toString();
			downloadAndRequestExtraction(address, null);
		}
	};

	private void initGui() {
		comboAddress.setEditable(true);
		comboAddress.setMaximumRowCount(10);
		comboAddress.setModel(new DefaultComboBoxModel());
		comboAddress.addItem("http://doisong.vnexpress.net/tin-tuc/nha-dep/tu-van-nha-dep/9-kieu-ket-hop-ban-ghe-trai-nguoc-phong-cach-3010427.html");
		comboAddress.addItem("http://worldcup.dantri.com.vn/world-cup-2014/cac-ung-vien-tiem-nang-vo-dich-world-cup-2014-893752.htm");
		comboAddress.addItem("http://www.huffingtonpost.com/2014/06/28/aereo-suspension-operatio_n_5539559.html");
		comboAddress.addItem("http://www.metronews.fr/conso/soldes-d-ete-2014-10-pieces-femme-a-s-offrir-d-urgence-chez-zara/mnfz!vD1qVKnYKBVTk/");
		comboAddress.addItem("http://www.metronews.fr/info/meteo-france-32-departements-en-alerte-orange-a-cause-des-orages/mnfB!KGqdLzKKCaCU/");
		comboAddress.addItem("http://www.lesechos.fr/finance-marches/banque-assurances/0203602724767-bnp-paribas-aux-etats-unis-bonnafe-reconnait-des-dysfonctionnements-et-des-erreurs-1019225.php");
		comboAddress.addItem("http://business.lesechos.fr/directions-ressources-humaines/partenaire/partenaire-160-enjeux-de-la-reforme-sur-la-formation-professionnelle-100753.php");
		comboAddress.addItem("http://vietnamnet.vn/vn/xa-hoi/183308/nhat-hoa-cuoi-roi-giua-duong-tai-xe-bi-thuong-nang.html");
		comboAddress.addActionListener(loadAddress);

		buttonGo.addActionListener(loadAddress);
		//buttonGo.setMaximumSize(new Dimension(100, buttonGo.getMaximumSize().height));

		JTabbedPane tabbedWebResult = new JTabbedPane();
		tabbedWebResult.addTab("Final", webResult);
		tabbedWebResult.addTab("Decoration", webResultDecorated);
		tabbedWebResult.addTab("Original", webOriginal);
		tabbedWebResult.addTab("Pre-Process", webPreProcess);
		tabbedWebResult.addTab("Ignore Lang", webResultIgnoreLang);
		tabbedWebResult.addTab("Ignore Lang Decoration ", webResultIgnoreLangDecorated);

		JPanel addressPanel = new JPanel();
		{
			addressPanel.setLayout(new BorderLayout());
			addressPanel.add(comboAddress, BorderLayout.CENTER);
			addressPanel.add(buttonGo, BorderLayout.EAST);
		}
		JPanel configsPanel = new JPanel();
		{
			DesignGridLayout layout = new DesignGridLayout(configsPanel);
			layout.row().grid().add(configPanel1, configPanel2, configPanel3);
		}
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		{
			splitPanel.setLeftComponent(configsPanel);
			splitPanel.setRightComponent(tabbedWebResult);
		}
		this.setLayout(new BorderLayout());
		{
			this.add(addressPanel, BorderLayout.PAGE_START);
			this.add(splitPanel, BorderLayout.CENTER);
		}
	}

	@Subscribe
	public void onConfigurationChanged(final Configuration config) {
		Log.debug(MainApp.EventBusMarker, "onConfigurationChanged", config);
		final String address = comboAddress.getSelectedItem().toString();
		addUrlHistory(address);
		downloadAndRequestExtraction(address, config);
	}

	private void downloadAndRequestExtraction(final String address, final Configuration config) {
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				downloadPage(address);
				return null;
			}
			@Override
			protected void done() {
				try {
					MainApp.EVENT_BUS.post(new ExtractionRequest(document_, config));
				}
				catch (Exception ex) {
					Log.error("Failed", ex);
				}
			}
		}.execute();
	}

	private void downloadPage(String address) throws IOException {
		Stopwatch sw = Stopwatch.createStarted();
		Connection con = HttpConnection.connect(new URL(address));
		con.userAgent(MainApp.MOBILE_USER_AGENT);
		document_ = con.get();
		sw.stop();
		Log.info(String.format("Download and parse: %d ms - '%s'", sw.elapsed(TimeUnit.MILLISECONDS), address));
	}

	private void addUrlHistory(String address) {
		for (int i=0; i< comboAddress.getItemCount(); i++) {
			if (comboAddress.getItemAt(i).toString().equalsIgnoreCase(address)) {
				return;
			}
		}
		comboAddress.addItem(address);
	}

//	@Subscribe
//	public void extractionStart(final Document document) {
//		Log.debug(MainApp.EventBusMarker, "extractionStart");
//
//	}
//
//	@Subscribe
//	public void extractionEnd(final Document document) {
//		Log.debug(MainApp.EventBusMarker, "extractionEnd");
//
//	}
}
