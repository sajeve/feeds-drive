package dh.newspaper.view;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import de.greenrobot.event.EventBus;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.modules.AppBundle;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.model.FeedItem;

import javax.inject.Inject;

/**
 * Article Reader
 */
public class ArticleFragment extends Fragment {
	private static final String TAG = ArticleFragment.class.getName();

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String SOURCE_PARAM = "source";

	@Inject
	ContentParser contentParser;

	@Inject
	AppBundle mAppBundle;

    private FeedItem mFeedItem;
	private TextView mTextView;

    public ArticleFragment() {
        // Required empty public constructor
		setRetainInstance(true);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_article, container, false);
		mTextView = (TextView) v.findViewById(R.id.fa_content);
		setFeedItem(mFeedItem);
		return v;
    }

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		EventBus.getDefault().unregister(this);
	}

	private boolean mFirstAttach = true;

	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		// make sure it's the first time through; we don't want to re-inject a retained fragment that is going
		// through a detach/attach sequence.
		if (mFirstAttach) {
			((Injector) activity.getApplication()).inject(this);
			mFirstAttach = false;
		}
		//mFeedItem = mAppBundle.getCurrentFeedItem();
	}

	public void onEvent(FeedsFragment.Event e) {
		try {
			if (!FeedsFragment.Event.ON_ITEM_SELECTED.equals(e.getSubject())) {
				return;
			}
			setFeedItem(e.getFeedItem());
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), "FeedsFragment.Event.ON_ITEM_SELECTED", ex);
		}
	}

	public void setFeedItem(FeedItem feedItem) {
		mFeedItem = feedItem;
		if (mTextView!=null && feedItem!=null) {
			mTextView.setText(feedItem.getDescription());
		}
	}

	public class Event extends BaseEventOneArg<ArticleFragment> {
		public FeedItem rssItem;
		public Event() {
			super(ArticleFragment.this);
		}
		public Event(String subject) {
			super(ArticleFragment.this, subject);
		}
	}
}
