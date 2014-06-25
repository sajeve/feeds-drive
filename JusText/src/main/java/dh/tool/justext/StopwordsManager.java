package dh.tool.justext;

import org.jsoup.helper.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by hiep on 24/06/2014.
 */
public class StopwordsManager {
	public static String getLanguage(String lang) {
		if (lang==null) {
			return null;
		}
		return languageMap.get(lang.toLowerCase());
	}

	static SortedSet<String> getStopwords(String language) throws IOException {
		if (!stopwords.containsKey(language)) {
			SortedSet<String> words = loadStopwords(language);
			stopwords.put(language, words);
		}
		return stopwords.get(language);
	}

	private static SortedSet<String> loadStopwords(String language) throws IOException {
		SortedSet<String> words = new TreeSet<String>();

		InputStream is = StopwordsManager.class.getResourceAsStream("/stopwords/"+language+".txt");
		BufferedReader buf = new BufferedReader(new InputStreamReader(is));
		try {
			String s;
			while ((s = buf.readLine()) != null) {
				if (!StringUtil.isBlank(s)) {
					words.add(s);
				}
			}
		}
		finally {
			buf.close();
			is.close();
		}

		return words;
	}

	private static final HashMap<String, SortedSet<String>> stopwords = new HashMap<String, SortedSet<String>>();


	/**
	 * map 'vn', 'vietnam', 'vn-vn' to Vietnam
	 */
	private static final HashMap<String, String> languageMap = new HashMap<String, String>() {{
		put("vietnamese", "Vietnamese");
		put("vn", "Vietnamese");
		put("vn-vn", "Vietnamese");
		put("vietnam", "Vietnamese");

		put("english", "English");
		put("simple_english", "Simple_English");
		put("en", "Simple_English");
		put("uk", "Simple_English");
		put("us", "Simple_English");
		put("en-au", "Simple_English");
		put("en-bz", "Simple_English");
		put("en-ca", "Simple_English");
		put("en-ie", "Simple_English");
		put("en-jm", "Simple_English");
		put("en-nz", "Simple_English");
		put("en-ph", "Simple_English");
		put("en-za", "Simple_English");
		put("en-tt", "Simple_English");
		put("en-gb", "Simple_English");
		put("en-us", "Simple_English");
		put("en-uk", "Simple_English");
		put("en-zw", "Simple_English");

		put("french", "French");
		put("fr", "French");
		put("fr-be", "French");
		put("fr-ca", "French");
		put("fr-fr", "French");
		put("fr-lu", "French");
		put("fr-mc", "French");
		put("fr-ch", "French");

		put("dutch", "Dutch");
		put("de", "Dutch");
		put("de-at", "Dutch");
		put("de-de", "Dutch");
		put("de-li", "Dutch");
		put("de-lu", "Dutch");
		put("de-ch", "Dutch");

		put("italian", "Italian");
		put("it", "Italian");
		put("it-ch", "Italian");

		put("portuguese", "Portuguese");
		put("pt", "Portuguese");
		put("pt-br", "Portuguese");
		put("pt-pt", "Portuguese");

		put("russian", "Russian");
		put("ru", "Russian");
		put("ru-mo", "Russian");
		put("ru-ru", "Russian");

		put("spanish", "Spanish");
		put("es", "Spanish");
		put("es-ar", "Spanish");
		put("es-bo", "Spanish");
		put("es-cl", "Spanish");
		put("es-co", "Spanish");
		put("es-cr", "Spanish");
		put("es-do", "Spanish");
		put("es-ec", "Spanish");
		put("es-sv", "Spanish");
		put("es-gt", "Spanish");
		put("es-hn", "Spanish");
		put("es-mx", "Spanish");
		put("es-ni", "Spanish");
		put("es-pa", "Spanish");
		put("es-py", "Spanish");
		put("es-pe", "Spanish");
		put("es-pr", "Spanish");
		put("es-es", "Spanish");
		put("es-uy", "Spanish");
		put("es-ve", "Spanish");

		put("chinese", "Chinese");
		put("zh-cn", "Chinese");
		put("zh-tw", "Chinese");

		put("japanese", "Japanese");
		put("ja", "Japanese");
		put("jp", "Japanese");

		put("korean", "Korean");
		put("ko", "Korean");
		put("kr", "Korean");

		put("indonesian", "Indonesian");
		put("in", "Indonesian");

		put("malay", "Malay");

		put("afrikaans", "Afrikaans");
		put("albanian", "Albanian");
		put("arabic", "Arabic");
		put("aragonese", "Aragonese");
		put("armenian", "Armenian");
		put("aromanian", "Aromanian");
		put("asturian", "Asturian");
		put("azerbaijani", "Azerbaijani");
		put("basque", "Basque");
		put("belarusian", "Belarusian");
		put("belarusian_taraskievica", "Belarusian_Taraskievica");
		put("bengali", "Bengali");
		put("bishnupriya_manipuri", "Bishnupriya_Manipuri");
		put("bosnian", "Bosnian");
		put("breton", "Breton");
		put("bulgarian", "Bulgarian");
		put("catalan", "Catalan");
		put("cebuano", "Cebuano");
		put("chuvash", "Chuvash");
		put("croatian", "Croatian");
		put("czech", "Czech");
		put("danish", "Danish");
		put("esperanto", "Esperanto");
		put("estonian", "Estonian");
		put("finnish", "Finnish");
		put("galician", "Galician");
		put("georgian", "Georgian");
		put("german", "German");
		put("greek", "Greek");
		put("gujarati", "Gujarati");
		put("haitian", "Haitian");
		put("hebrew", "Hebrew");
		put("hindi", "Hindi");
		put("hungarian", "Hungarian");
		put("icelandic", "Icelandic");
		put("ido", "Ido");
		put("igbo", "Igbo");
		put("irish", "Irish");
		put("kannada", "Kannada");
		put("kurdish", "Kurdish");
		put("latin", "Latin");
		put("latvian", "Latvian");
		put("lithuanian", "Lithuanian");
		put("lombard", "Lombard");
		put("low_saxon", "Low_Saxon");
		put("luxembourgish", "Luxembourgish");
		put("macedonian", "Macedonian");
		put("malayalam", "Malayalam");
		put("maltese", "Maltese");
		put("marathi", "Marathi");
		put("neapolitan", "Neapolitan");
		put("nepali", "Nepali");
		put("newar", "Newar");
		put("norwegian_bokmal", "Norwegian_Bokmal");
		put("norwegian_nynorsk", "Norwegian_Nynorsk");
		put("occitan", "Occitan");
		put("persian", "Persian");
		put("piedmontese", "Piedmontese");
		put("polish", "Polish");
		put("quechua", "Quechua");
		put("romanian", "Romanian");
		put("samogitian", "Samogitian");
		put("serbian", "Serbian");
		put("serbo_croatian", "Serbo_Croatian");
		put("sicilian", "Sicilian");
		put("slovak", "Slovak");
		put("slovenian", "Slovenian");
		put("sundanese", "Sundanese");
		put("swahili", "Swahili");
		put("swedish", "Swedish");
		put("tagalog", "Tagalog");
		put("tamil", "Tamil");
		put("telugu", "Telugu");
		put("turkish", "Turkish");
		put("ukrainian", "Ukrainian");
		put("urdu", "Urdu");
		put("volapuk", "Volapuk");
		put("walloon", "Walloon");
		put("waray_waray", "Waray_Waray");
		put("welsh", "Welsh");
		put("west_frisian", "West_Frisian");
		put("western_panjabi", "Western_Panjabi");
		put("yoruba", "Yoruba");
	}};
}