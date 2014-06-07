package dh.newspaper.event;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshTagsListEvent<BackgroundTasksManager> extends BaseEvent<BackgroundTasksManager> {
	public RefreshTagsListEvent(BackgroundTasksManager sender, String subject) {
		super(sender, subject);
	}

}