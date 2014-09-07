package dh.newspaper.event;

import dh.newspaper.workflow.SelectArticleWorkflow;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getActiveTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshArticleEvent extends BaseEvent<SelectArticleWorkflow> {
	public final String OverrideArticleContent;
	public RefreshArticleEvent(SelectArticleWorkflow sender, String subject, String flowId) {
		super(sender, subject, flowId);
		this.OverrideArticleContent = null;
	}
	public RefreshArticleEvent(SelectArticleWorkflow sender, String subject, String flowId, String overrideArticleContent) {
		super(sender, subject, flowId);
		this.OverrideArticleContent = overrideArticleContent;
	}
}
