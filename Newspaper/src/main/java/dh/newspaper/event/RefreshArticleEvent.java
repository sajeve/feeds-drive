package dh.newspaper.event;

import dh.newspaper.model.generated.Article;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.workflow.SelectArticleWorkflow;

import java.util.UUID;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshArticleEvent extends BaseEvent<SelectArticleWorkflow> {
	public RefreshArticleEvent(SelectArticleWorkflow sender, String subject, UUID flowId) {
		super(sender, subject, flowId);
	}
}
