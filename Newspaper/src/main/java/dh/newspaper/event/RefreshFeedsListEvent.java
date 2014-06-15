package dh.newspaper.event;

import dh.newspaper.model.generated.Article;
import dh.newspaper.workflow.SelectTagWorkflow;

import java.util.List;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshFeedsListEvent extends BaseEvent<SelectTagWorkflow> {
	private final List<Article> mArticles;
	private final int mCount;

	public RefreshFeedsListEvent(SelectTagWorkflow sender, String subject, String flowId, List<Article> articles, int count) {
		super(sender, subject, flowId);
		mArticles = articles;
		mCount = count;
	}

	public List<Article> getArticles() {
		return mArticles;
	}

	public int getCount() {
		return mCount;
	}
}
