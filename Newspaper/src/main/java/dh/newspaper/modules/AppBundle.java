package dh.newspaper.modules;

import android.util.Log;
import dh.newspaper.model.FeedItem;
import dh.newspaper.view.TagsFragment;
import dh.newspaper.view.FeedsFragment;

import javax.inject.Inject;

/**
 * Store all information to initialize fragment/activity which is not ready to receive event from EventBus
 * onEvent here has a higher priority (100) than default (0)
 * Created by hiep on 17/05/2014.
 */
public class AppBundle {
	private static final String TAG = AppBundle.class.getName();

	private FeedItem currentFeedItem;
	private int currentTagPos;

	@Inject
	public AppBundle() {}

	public void onEvent(FeedsFragment.Event e) {
		try {
			if (!FeedsFragment.Event.ON_ITEM_SELECTED.equals(e.getSubject())) {
				return;
			}
			currentFeedItem = e.getFeedItem();
		}catch (Exception ex) {
			Log.w(TAG, ex);
		}
	}

	public void onEvent(TagsFragment.Event e) {
		try {
			currentTagPos = e.getTagPos();
		} catch (Exception ex) {
			Log.w(TAG, ex);
		}
	}

	public FeedItem getCurrentFeedItem() {
		return currentFeedItem;
	}

	public int getCurrentTagPos() {
		return currentTagPos;
	}
}
