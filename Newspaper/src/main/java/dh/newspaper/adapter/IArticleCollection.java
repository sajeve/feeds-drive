package dh.newspaper.adapter;

import dh.newspaper.model.generated.Article;

import java.util.List;

/**
 * Technique to navigate a very long list in the disk cache database but keeping a
 * small list of article (buffer) in memory. Using paging (or window) mechanism
 * Created by hiep on 14/06/2014.
 */
public interface IArticleCollection {
	/**
	 * the total size of the real list
	 */
	public int getTotalSize();

	/**
	 * return true if the item is loaded in memory
	 */
	public boolean isInMemoryCache(int position);

	/**
	 * get any item in the list and move "page" (or window) to the position
	 */
	public Article getArticle(int position);

	public static interface OnInMemoryCacheChangeCallback {
		public void onChanged(IArticleCollection sender, List<Article> buffer, int offset, int pageSize);
	}
}
