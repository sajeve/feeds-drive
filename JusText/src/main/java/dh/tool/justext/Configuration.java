package dh.tool.justext;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@link dh.tool.justext.Configuration} is read-only.
 * Use {@link dh.tool.justext.Configuration.Builder} to build it
 * Created by hiep on 24/06/2014.
 */
public class Configuration implements Serializable, Cloneable {
	public static final boolean DEBUG = true;
	public static final Configuration DEFAULT = new Configuration();

	private boolean processHeadings = true;
	private int maxHeadingDistance = 200;
	private int lengthLow = 70;
	private int lengthHigh = 200;
	private double stopwordsLow = 0.3;
	private double stopwordsHigh = 0.32;
	private double maxLinkDensity = 0.2;
	private boolean strictOnEdgeContent = true;
	private boolean removeTitle = false;
	private boolean preCleanUselessContent = true;
	private boolean postCleanBoilerplateTags = true;
	private boolean processOnlyBody = true;
	private String language;
	private boolean autoDetectLanguage = true;
	private boolean contentAlwaysHasTitle = true;

	public boolean processHeadings() {
		return processHeadings;
	}
	public int maxHeadingDistance() {
		return maxHeadingDistance;
	}
	public int lengthLow() {
		return lengthLow;
	}
	public int lengthHigh() {
		return lengthHigh;
	}
	public double stopwordsLow() {
		if (language==null) {
			return 0;
		}
		return stopwordsLow;
	}
	public double stopwordsHigh() {
		if (language==null) {
			return 0;
		}
		return stopwordsHigh;
	}
	public double maxLinkDensity() {
		return maxLinkDensity;
	}
	public String language() {
		return language;
	}
	public boolean strictOnEdgeContent() {
		return strictOnEdgeContent;
	}
	public boolean removeTitle() {
		return removeTitle;
	}
	public boolean preCleanUselessContent() {
		return preCleanUselessContent;
	}
	public boolean postCleanBoilerplateTags() {
		return postCleanBoilerplateTags;
	}
	/**
	 * Keep head tag out-side of the process
	 */
	public boolean processOnlyBody() {
		return processOnlyBody;
	}
	public boolean autoDetectLanguage() {
		return autoDetectLanguage;
	}
	public boolean contentAlwaysHasTitle() {
		return contentAlwaysHasTitle;
	}

	@Override
	protected Configuration clone() throws CloneNotSupportedException {
		Configuration resu = new Configuration();
		resu.processHeadings = processHeadings;
		resu.maxHeadingDistance = maxHeadingDistance;
		resu.lengthLow = lengthLow;
		resu.lengthHigh = lengthHigh;
		resu.stopwordsLow = stopwordsLow;
		resu.stopwordsHigh = stopwordsHigh;
		resu.maxLinkDensity = maxLinkDensity;
		resu.language = language;
		resu.strictOnEdgeContent = strictOnEdgeContent;
		resu.removeTitle = removeTitle;
		resu.preCleanUselessContent = preCleanUselessContent;
		resu.postCleanBoilerplateTags = postCleanBoilerplateTags;
		resu.processOnlyBody = processOnlyBody;
		resu.autoDetectLanguage = autoDetectLanguage;
		resu.contentAlwaysHasTitle = contentAlwaysHasTitle;
		return resu;
	}

	private void loadConfig(CharSequence str) throws ParseException {
		Iterator<String> it = Splitter.on(Pattern.compile(";\\s")).trimResults().omitEmptyStrings().split(str).iterator();
		while (it.hasNext()) {
			Iterator<String> p = Splitter.on("=").trimResults().omitEmptyStrings().limit(2).split(it.next()).iterator();
			String key = p.next();
			String value = p.next().toLowerCase();

			NumberFormat format = NumberFormat.getInstance(Locale.US);
			if ("processHeadings".equalsIgnoreCase(key)) processHeadings = Boolean.parseBoolean(value);
			if ("maxHeadingDistance".equalsIgnoreCase(key)) maxHeadingDistance = Integer.parseInt(value);
			if ("lengthLow".equalsIgnoreCase(key)) lengthLow = Integer.parseInt(value);
			if ("lengthHigh".equalsIgnoreCase(key)) lengthHigh = Integer.parseInt(value);
			if ("stopwordsLow".equalsIgnoreCase(key)) stopwordsLow = format.parse(value).doubleValue();
			if ("stopwordsHigh".equalsIgnoreCase(key)) stopwordsHigh = format.parse(value).doubleValue();
			if ("maxLinkDensity".equalsIgnoreCase(key)) maxLinkDensity = format.parse(value).doubleValue();
			if ("strictOnEdgeContent".equalsIgnoreCase(key)) strictOnEdgeContent = Boolean.parseBoolean(value);
			if ("removeTitle".equalsIgnoreCase(key)) removeTitle = Boolean.parseBoolean(value);
			if ("preCleanUselessContent".equalsIgnoreCase(key)) preCleanUselessContent = Boolean.parseBoolean(value);
			if ("postCleanBoilerplateTags".equalsIgnoreCase(key)) postCleanBoilerplateTags = Boolean.parseBoolean(value);
			if ("processOnlyBody".equalsIgnoreCase(key)) processOnlyBody = Boolean.parseBoolean(value);
			if ("language".equalsIgnoreCase(key)) language = StopwordsManager.getLanguage(value);
			if ("autoDetectLanguage".equalsIgnoreCase(key)) autoDetectLanguage = Boolean.parseBoolean(value);
			if ("contentAlwaysHasTitle".equalsIgnoreCase(key)) contentAlwaysHasTitle = Boolean.parseBoolean(value);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("processHeadings = " + processHeadings + "; ");
		sb.append("maxHeadingDistance = " + maxHeadingDistance + "; ");
		sb.append("lengthLow = " + lengthLow + "; ");
		sb.append("lengthHigh = " + lengthHigh + "; ");
		sb.append("stopwordsLow = " + stopwordsLow + "; ");
		sb.append("stopwordsHigh = " + stopwordsHigh + "; ");
		sb.append("maxLinkDensity = " + maxLinkDensity + "; ");
		sb.append("strictOnEdgeContent = " + strictOnEdgeContent + "; ");
		sb.append("removeTitle = " + removeTitle + "; ");
		sb.append("preCleanUselessContent = " + preCleanUselessContent + "; ");
		sb.append("postCleanBoilerplateTags = " + postCleanBoilerplateTags + "; ");
		sb.append("processOnlyBody = " + processOnlyBody + "; ");
		sb.append("language = " + language + "; ");
		sb.append("autoDetectLanguage = " + autoDetectLanguage + "; ");
		sb.append("contentAlwaysHasTitle = " + contentAlwaysHasTitle + "; ");
		return sb.toString().trim();
	}

	/**
	 * Config can be load from a String. example:
	 * "processHeadings = true; maxLinkDensity = 0.2"
	 */
	public static class Builder implements Cloneable {
		Configuration configuration;

		public Builder() {
			configuration = new Configuration();
		}

		public Builder(Configuration fromConfiguration) {
			try {
				this.configuration = fromConfiguration.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}

		public Builder(CharSequence fromConfigString) throws ParseException {
			this();
			configuration.loadConfig(fromConfigString);
		}

		Builder load(CharSequence configString) throws ParseException {
			configuration.loadConfig(configString);
			return this;
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return new Builder(configuration.clone());
		}

		public Configuration build() {
			try {
				return configuration.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			throw new IllegalStateException("cannot clone");
		}

		private Builder processHeadings(boolean processHeadings) {
			configuration.processHeadings = processHeadings;
			return this;
		}

		public Builder maxHeadingDistance(int maxHeadingDistance) {
			configuration.maxHeadingDistance = maxHeadingDistance;
			return this;
		}

		public Builder lengthLow(int lengthLow) {
			configuration.lengthLow = lengthLow;
			return this;
		}

		public Builder lengthHigh(int lengthHigh) {
			configuration.lengthHigh = lengthHigh;
			return this;
		}

		public Builder stopwordsLow(double stopwordsLow) {
			configuration.stopwordsLow = stopwordsLow;
			return this;
		}

		public Builder stopwordsHigh(double stopwordsHigh) {
			configuration.stopwordsHigh = stopwordsHigh;
			return this;
		}

		public Builder maxLinkDensity(double maxLinkDensity) {
			configuration.maxLinkDensity = maxLinkDensity;
			return this;
		}

		public Builder language(String language) {
			configuration.language = StopwordsManager.getLanguage(language);
			return this;
		}

		public Builder strictOnEdgeContent(boolean removeEdgeContent) {
			configuration.strictOnEdgeContent = removeEdgeContent;
			return this;
		}

		public Builder removeTitle(boolean removeTitle) {
			configuration.removeTitle = removeTitle;
			return this;
		}

		public Builder cleanUselessTag(boolean cleanUselessTag) {
			configuration.preCleanUselessContent = cleanUselessTag;
			return this;
		}
		public Builder cleanBoilerplatesTags(boolean cleanEmptyTag) {
			configuration.postCleanBoilerplateTags = cleanEmptyTag;
			return this;
		}
		public Builder processOnlyBody(boolean processOnlyBody) {
			configuration.processOnlyBody = processOnlyBody;
			return this;
		}
		public Builder autoDetectLanguage(boolean autoDetectLanguage) {
			configuration.autoDetectLanguage = autoDetectLanguage;
			return this;
		}
		public Builder contentAlwaysHasTitle(boolean contentAlwaysHasTitle) {
			configuration.contentAlwaysHasTitle = contentAlwaysHasTitle;
			return this;
		}
	}
}