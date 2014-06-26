package dh.tool.justext;

import java.io.Serializable;

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
	private boolean removeEdgeContent = true;
	private boolean removeTitle = false;
	private boolean preCleanUselessContent = true;
	private boolean postCleanBoilerplateTags = true;
	private boolean processOnlyBody = true;
	private String language;
	private boolean autoDetectLanguage = true;

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

	public boolean removeEdgeContent() {
		return removeEdgeContent;
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
	 * Keep head tag out-side of the business
	 */
	public boolean processOnlyBody() {
		return processOnlyBody;
	}

	public boolean autoDetectLanguage() {
		return autoDetectLanguage;
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
		resu.removeEdgeContent = removeEdgeContent;
		resu.removeTitle = removeTitle;
		resu.preCleanUselessContent = preCleanUselessContent;
		resu.postCleanBoilerplateTags = postCleanBoilerplateTags;
		resu.processOnlyBody = processOnlyBody;
		resu.autoDetectLanguage = autoDetectLanguage;
		return resu;
	}

	public static class Builder implements Cloneable {
		Configuration configuration;

		public Builder() {
			configuration = new Configuration();
		}

		public Builder(Configuration fromConfiguration) throws CloneNotSupportedException {
			this.configuration = fromConfiguration;
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

		public Builder removeEdgeContent(boolean removeEdgeContent) {
			configuration.removeEdgeContent = removeEdgeContent;
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
	}
}