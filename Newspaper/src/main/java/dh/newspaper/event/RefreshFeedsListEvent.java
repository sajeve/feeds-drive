package dh.newspaper.event;

import de.greenrobot.dao.query.LazyList;
import dh.newspaper.model.generated.Article;
import dh.newspaper.workflow.SelectTagWorkflow;

import java.util.UUID;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshFeedsListEvent extends BaseEvent<SelectTagWorkflow> {
	private final LazyList<Article> mArticles;
	private final int mCount;

	public RefreshFeedsListEvent(SelectTagWorkflow sender, String subject, UUID flowId, LazyList<Article> articles, int count) {
		super(sender, subject, flowId);
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
