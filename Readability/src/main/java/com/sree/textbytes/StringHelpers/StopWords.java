package com.sree.textbytes.StringHelpers;

import java.io.IOException;
import java.util.*;

import com.google.common.base.Strings;
import com.sree.textbytes.StringHelpers.string;

/**
 * Created By NLP Community
 *
 * @User 		: Sreejith.S
 *
 * List of stop words in English language.
 */

public class StopWords
{
	// the confusing pattern below is basically just match any non-word character excluding white-space.
	public static final StringReplacement PUNCTUATION = StringReplacement.compile("[^\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}\\p{Nd}\\p{Pc}\\s]", string.empty);

	public static String removePunctuation(String str) {
		return PUNCTUATION.replaceAll(str);
	}


	public static WordStats getStopWordCount(String content, String language) {
		if (string.isNullOrEmpty(content)) return WordStats.EMPTY;

		WordStats ws = new WordStats();



		String strippedInput = removePunctuation(content);
		String[] words = string.SPACE_SPLITTER.split(strippedInput);

		if (Strings.isNullOrEmpty(language)) {
			ws.setStopWordCount(0);
			ws.setStopWords(new ArrayList<String>());
		}
		else {
			SortedSet<String> stopwordsList = null;
			try {
				stopwordsList = StopwordsManager.loadStopwords(language);
				//stem each word in the array if it is not null or a stop word
				List<String> stopWords = new ArrayList<String>();
				for (int i = 0; i < words.length; i++) {
					String word = words[i];
					if (string.isNullOrEmpty(word)) continue;
					String wordLower = word.toLowerCase();
					if (stopwordsList.contains(wordLower))
						stopWords.add(wordLower);
				}
				ws.setStopWordCount(stopWords.size());
				ws.setStopWords(stopWords);
			} catch (IOException e) {
				e.printStackTrace();
				ws.setStopWordCount(0);
				ws.setStopWords(new ArrayList<String>());
			}
		}
		ws.setWordCount(words.length);
		return ws;
	}
  
/*  public Set<String> getStopWords() {
	  return this.STOP_WORDS;
  }
  
  public static String removeStopWords(String str) {
	  return str.replaceAll(STOP_WORDS.toString(), string.empty);
  }*/


}