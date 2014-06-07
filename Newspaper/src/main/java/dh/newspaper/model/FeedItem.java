package dh.newspaper.model;

import com.google.common.base.Strings;
import dh.newspaper.Constants;
import dh.newspaper.tools.StrUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.Serializable;

/**
 * Created by hiep on 7/05/2014.
 */
public class FeedItem implements Serializable {
	private String mParentUrl;
	private String mTitle;
	private String mPublishedDate;
	private String mDescription;
	private String mImageUrl;
	private String mExcerpt;
	private String mUri;
	private String mLanguage;
	private String mAuthor;

	public FeedItem(String parentUrl, String title, String publishedDate, String description, String uri, String language, String author) {
		this.mTitle = title;
		this.mPublishedDate = publishedDate;
		this.mDescription = description;
		this.mUri = uri;
		this.mLanguage = language;
		this.mParentUrl = parentUrl;
		this.mAuthor = author;
	}

	public void initImageAndExcerpt() {
		if (Strings.isNullOrEmpty(mDescription)){
			return;
		}

		Document doc = Jsoup.parse(mDescription, mUri);

		//set mExcerpt text
		String descriptionTextOnly = doc.text();
		mExcerpt = descriptionTextOnly.substring(0, Math.min(descriptionTextOnly.length(), Constants.EXCERPT_LENGTH));

		//set image URL
		Elements elems = doc.select("img");
		if (elems == null) {
			return;
		}
		Element elem = elems.first();
		if (elem == null) {
			return;
		}

		mImageUrl = elem.attr("abs:src");
	}

	public String getTitle() {
		return mTitle;
	}

	public String getPublishedDate() {
		return mPublishedDate;
	}

	public String getDescription() {
		return mDescription;
	}

	public String getUri() {
		return mUri;
	}

	public String getLanguage() {
		return mLanguage;
	}

	public String getParentUrl() {
		return mParentUrl;
	}

	public String getAuthor() {
		return mAuthor;
	}

	public String getImageUrl() {
		return mImageUrl;
	}

	public String getExcerpt() {
		return mExcerpt;
	}

	@Override
	public String toString() {
		return String.format("[FeedItem: '%s' '%s' parent='%s' published=]", getTitle(), getUri(), getParentUrl(), getPublishedDate());
	}
}
