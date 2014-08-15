package dh.newspaper.tools;

import android.text.TextUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import dh.tool.common.StrUtils;

/**
 * Created by hiep on 13/08/2014.
 */
public class TagUtils {
	public static String getTechnicalTags(Iterable<String> tags) {
		if (tags==null) {
			return null;
		}
		StringBuilder sb = new StringBuilder("|");
		for (String tag : tags) {
			sb.append(normalizeTag(tag)+"|");
		}

		return sb.toString();
	}
	public static String getTechnicalTags(String[] tags) {
		if (tags==null) {
			return null;
		}
		StringBuilder sb = new StringBuilder("|");
		for (String tag : tags) {
			sb.append(normalizeTag(tag)+"|");
		}

		return sb.toString();
	}

	public static String getTechnicalTag(String tag) {
		return "|"+normalizeTag(tag)+"|";
	}

	public static String normalizeTag(String tag) {
		 return StrUtils.normalizeUpper(tag).replace('|', ' ');
	}

	/**
	 * convert from "|NEWS|HOME|WORLD ASIAN|HI-TECH|" to "news, home, world, asian, hi-tech"
	 * @param tags
	 * @return
	 */
	public static String getPrintableLowerCasesTags(String tags) {
		if (TextUtils.isEmpty(tags)) {
			return null;
		}
		return Joiner.on(", ").join(Splitter.on('|').omitEmptyStrings()
				.split(tags.toLowerCase()));
	}
}
