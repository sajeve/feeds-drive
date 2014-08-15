package dh.newspaper.event;

/**
 * Tell GUI to update tags list base on {@link dh.newspaper.cache.RefData#getActiveTags()}
 * Created by hiep on 4/06/2014.
 */
public class RefreshTagsListEvent extends RawEvent {
	public RefreshTagsListEvent(String subject) {
		super(subject);
	}

}
