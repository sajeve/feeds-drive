package dh.newspaper.event;

import dh.newspaper.model.generated.Article;
import dh.newspaper.workflow.SelectArticleWorkflow;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshArticleEvent<BackgroundTasksManager> extends BaseEvent<BackgroundTasksManager> {
	SelectArticleWorkflow mSelectArticleWorkflow;

	public RefreshArticleEvent(BackgroundTasksManager sender, String subject, SelectArticleWorkflow data) {
		super(sender, subject);
		mSelectArticleWorkflow = data;
	}

	public SelectArticleWorkflow getData() {
		return mSelectArticleWorkflow;
	}
}
