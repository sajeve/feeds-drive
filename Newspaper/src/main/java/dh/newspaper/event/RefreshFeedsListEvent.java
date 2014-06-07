package dh.newspaper.event;

import de.greenrobot.dao.query.LazyList;
import dh.newspaper.model.generated.Article;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshFeedsListEvent<BackgroundTasksManager> extends BaseEvent<BackgroundTasksManager> {
	private final LazyList<Article> mArticles;
	private final int mCount;

	public RefreshFeedsListEvent(BackgroundTasksManager sender, String subject, LazyList<Article> articles, int count) {
		super(sender, subject);
		mArticles = articles;
		mCount = count;
	}

	public LazyList<Article> getArticles() {
		return mArticles;
	}

	public int getCount() {
		return mCount;
	}
}
