package dh.newspaper.event;

import dh.newspaper.workflow.SelectTagWorkflow;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getActiveTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshFeedsListEvent extends BaseEvent<SelectTagWorkflow> {
	public RefreshFeedsListEvent(SelectTagWorkflow sender, String subject, String flowId) {
		super(sender, subject, flowId);
	}
}
