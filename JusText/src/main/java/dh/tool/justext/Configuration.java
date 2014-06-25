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
	private boolean cleanUselessTag = true;
	private boolean cleanEmptyTag = true;
	private boolean processOnlyBody = true;
	private String language;

	public boolean isProcessHeadings() {
		return processHeadings;
	}

	private void setProcessHeadings(boolean processHeadings) {
		this.processHeadings = processHeadings;
	}

	public int getMaxHeadingDistance() {
		return maxHeadingDistance;
	}

	private void setMaxHeadingDistance(int maxHeadingDistance) {
		this.maxHeadingDistance = maxHeadingDistance;
	}

	public int getLengthLow() {
		return lengthLow;
	}

	private void setLengthLow(int lengthLow) {
		this.lengthLow = lengthLow;
	}

	public int getLengthHigh() {
		return lengthHigh;
	}

	private void setLengthHigh(int lengthHigh) {
		this.lengthHigh = lengthHigh;
	}

	public double getStopwordsLow() {
		if (language==null) {
			return 0;
		}
		return stopwordsLow;
	}

	private void setStopwordsLow(double stopwordsLow) {
		this.stopwordsLow = stopwordsLow;
	}

	public double getStopwordsHigh() {
		if (language==null) {
			return 0;
		}
		return stopwordsHigh;
	}

	private void setStopwordsHigh(double stopwordsHigh) {
		this.stopwordsHigh = stopwordsHigh;
	}

	public double getMaxLinkDensity() {
		return maxLinkDensity;
	}

	private void setMaxLinkDensity(double maxLinkDensity) {
		this.maxLinkDensity = maxLinkDensity;
	}

	public String getLanguage() {
		return language;
	}

	private void setLanguage(String language) {
		this.language = language;
	}

	public boolean isRemoveEdgeContent() {
		return removeEdgeContent;
	}

	private void setRemoveEdgeContent(boolean removeEdgeContent) {
		this.removeEdgeContent = removeEdgeContent;
	}

	public boolean isRemoveTitle() {
		return removeTitle;
	}

	private void setRemoveTitle(boolean removeTitle) {
		this.removeTitle = removeTitle;
	}

	public boolean isCleanUselessTag() {
		return cleanUselessTag;
	}

	public boolean isCleanEmptyTag() {
		return cleanEmptyTag;
	}

	public boolean isProcessOnlyBody() {
		return processOnlyBody;
	}

	@Override
	protected Configuration clone() throws CloneNotSupportedException {
		Configuration resu = new Configuration();
		resu.setProcessHeadings(processHeadings);
		resu.setMaxHeadingDistance(maxHeadingDistance);
		resu.setLengthLow(lengthLow);
		resu.setLengthHigh(lengthHigh);
		resu.setStopwordsLow(stopwordsLow);
		resu.setStopwordsHigh(stopwordsHigh);
		resu.setMaxLinkDensity(maxLinkDensity);
		resu.setLanguage(language);
		resu.setRemoveEdgeContent(removeEdgeContent);
		resu.setRemoveTitle(removeTitle);
		resu.cleanUselessTag = this.cleanUselessTag;
		resu.cleanEmptyTag = this.cleanEmptyTag;
		resu.processOnlyBody = this.processOnlyBody;
		return resu;
	}

	public static class Builder implements Cloneable {
		Configuration configuration;

		public Builder() {
			configuration = new Configuration();
		}

		public Builder(Configuration fromConfiguration) throws CloneNotSupportedException {
			this.configuration = configuration;
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

		private Builder setNoHeadings(boolean noHeadings) {
			configuration.setProcessHeadings(noHeadings);
			return this;
		}

		public Builder setMaxHeadingDistance(int maxHeadingDistance) {
			configuration.setMaxHeadingDistance(maxHeadingDistance);
			return this;
		}

		public Builder setLengthLow(int lengthLow) {
			configuration.setLengthLow(lengthLow);
			return this;
		}

		public Builder setLengthHigh(int lengthHigh) {
			configuration.setLengthHigh(lengthHigh);
			return this;
		}

		public Builder setStopwordsLow(double stopwordsLow) {
			configuration.setStopwordsLow(stopwordsLow);
			return this;
		}

		public Builder setStopwordsHigh(double stopwordsHigh) {
			configuration.setStopwordsHigh(stopwordsHigh);
			return this;
		}

		public Builder setMaxLinkDensity(double maxLinkDensity) {
			configuration.setMaxLinkDensity(maxLinkDensity);
			return this;
		}

		public Builder setLanguage(String language) {
			configuration.setLanguage(StopwordsManager.getLanguage(language));
			return this;
		}

		public Builder setRemoveEdgeContent(boolean removeEdgeContent) {
			configuration.setRemoveEdgeContent(removeEdgeContent);
			return this;
		}

		public Builder setRemoveTitle(boolean removeTitle) {
			configuration.setRemoveTitle(removeTitle);
			return this;
		}

		public Builder setCleanUselessTag(boolean cleanUselessTag) {
			configuration.cleanUselessTag = cleanUselessTag;
			return this;
		}
		public Builder setCleanEmptyTag(boolean cleanEmptyTag) {
			configuration.cleanEmptyTag = cleanEmptyTag;
			return this;
		}
		public Builder setProcessOnlyBody(boolean processOnlyBody) {
			configuration.processOnlyBody = processOnlyBody;
			return this;
		}
	}
}