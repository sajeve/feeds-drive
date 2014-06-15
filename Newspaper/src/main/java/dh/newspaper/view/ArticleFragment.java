package dh.newspaper.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.event.RefreshArticleEvent;
import dh.newspaper.model.FeedItem;
import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.tools.StrUtils;
import dh.newspaper.workflow.SelectArticleWorkflow;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Article Reader
 */
public class ArticleFragment extends Fragment {
	private static final String TAG = ArticleFragment.class.getName();

	/*@Inject
	AppBundle mAppBundle;*/

	@Inject
	BackgroundTasksManager mBackgroundTasksManager;

	private SwipeRefreshLayout mSwipeRefreshLayout;
	private WebView mWebView;
	private TextView mTxtTitle;
	private TextView mTxtDataSource;
	private TextView mTxtTags;
	private TextView mTxtNotice;
	private View mPanelNotice;

	private Article mArticle;

	/**
	 * use to trace the current pending event
	 */
	private String mEventFlowId;


    public ArticleFragment() {
		// Required empty public constructor
		super();
		//setRetainInstance(true);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
		mSwipeRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.fragment_article, container, false);
		mWebView = (WebView) mSwipeRefreshLayout.findViewById(R.id.webview_article);
		mTxtTitle = (TextView) mSwipeRefreshLayout.findViewById(R.id.txt_title);
		mTxtDataSource = (TextView) mSwipeRefreshLayout.findViewById(R.id.txt_date_source);
		mTxtTags = (TextView) mSwipeRefreshLayout.findViewById(R.id.txt_tags);
		mTxtNotice = (TextView) mSwipeRefreshLayout.findViewById(R.id.txt_notice);
		mPanelNotice = mSwipeRefreshLayout.findViewById(R.id.panel_notice);

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				try {
					mBackgroundTasksManager.loadArticle(mArticle);
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
					MyApplication.showErrorDialog(ArticleFragment.this.getFragmentManager(), "Failed refresh", ex);
				}
			}
		});

		return mSwipeRefreshLayout;
    }

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
		refreshGUI();
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
		//mArticle = mAppBundle.getCurrentArticle();
	}

	private static final String STATE_EVENT_FLOW_ID = "EventFlowId";
	private static final String STATE_ARTICLE = "Article";

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_ARTICLE, mArticle);
		outState.putString(STATE_EVENT_FLOW_ID, mEventFlowId);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		mArticle = (Article)savedInstanceState.getSerializable(STATE_ARTICLE);
		mEventFlowId = savedInstanceState.getString(STATE_EVENT_FLOW_ID);
	}

	/*public void onEventMainThread(FeedsFragment.Event event) {
		if (!isAdded()) {
			return;
		}
		try {
			if (!FeedsFragment.Event.SELECT_ITEM.equals(event.getSubject())) {
				return;
			}
			mBackgroundTasksManager.loadArticle(event.getArticle());
		}catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}*/

	Stopwatch swRae = Stopwatch.createStarted();
	public void onEventMainThread(RefreshArticleEvent event) {
		if (!isAdded() || mWebView ==null) {
			return;
		}
		try {
			switch (event.getSubject()) {
				case Constants.SUBJECT_ARTICLE_START_LOADING:
					if (Constants.DEBUG) {
						swRae = Stopwatch.createStarted();
						Log.d("dh.newspaper.DebugClick", "ArticleFragment START " + event.getSender().getArticleUrl() + " - " + event.getFlowId() + " (previous flow="+mEventFlowId+")");
					}

					mSwipeRefreshLayout.setRefreshing(true);

					//Save the identity of the current running workflow recognise future events coming from the same workflow
					mEventFlowId = event.getFlowId();

					return;
				case Constants.SUBJECT_ARTICLE_REFRESH:
					if (mEventFlowId != event.getFlowId()) {
						//this event is fired by a sender which is no more concerning by this fragment -> do nothing
						return;
					}

					if (Constants.DEBUG) {
						Log.d("dh.newspaper.DebugClick", "ArticleFragment REFRESH (" + swRae.elapsed(TimeUnit.MILLISECONDS) + " ms) " + event.getSender().getArticleUrl() + " - " + mEventFlowId);
						swRae.reset().start();
					}

					refreshGUI(event.getSender());
					return;
				case Constants.SUBJECT_ARTICLE_DONE_LOADING:
					if (mEventFlowId != event.getFlowId()) {
						//this event is fired by a sender which is no more concerning by this fragment -> do nothing
						return;
					}

					if (Constants.DEBUG)
						Log.d("dh.newspaper.DebugClick", "ArticleFragment DONE (" + swRae.elapsed(TimeUnit.MILLISECONDS) + " ms) " + event.getSender().getArticleUrl() + " - " + mEventFlowId);

					mSwipeRefreshLayout.setRefreshing(false);
					return;
			}
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}


	private void refreshGUI() {
		SelectArticleWorkflow selectArticleWorkflow = mBackgroundTasksManager.getActiveSelectArticleWorkflow();
		if (selectArticleWorkflow!=null) {
			refreshGUI(selectArticleWorkflow);
		}
		else {
			mBackgroundTasksManager.loadArticle(mArticle);
		}
	}
	private void refreshGUI(SelectArticleWorkflow data) {
		if (data == null) {
			if (Constants.DEBUG) {
				throw new IllegalStateException("data is null");
			}
			else {
				Log.w(TAG, "data is null");
			}
		}
		mArticle = data.getArticle();

		mTxtTitle.setText(mArticle.getTitle());
		mTxtTags.setText(getTagsInfo(data.getParentSubscription()));
		mTxtDataSource.setText(Html.fromHtml(getBasicInfo(mArticle)));

		String encoding=data.getParentSubscription() == null ? null : data.getParentSubscription().getEncoding();
		mWebView.loadDataWithBaseURL(mArticle.getArticleUrl(), mArticle.getContent(), "text/html", encoding, mArticle.getParentUrl());

		if (!Strings.isNullOrEmpty(mArticle.getParseNotice())) {
			mTxtNotice.setText(mArticle.getParseNotice() + "-" + mArticle.getArticleUrl());
			mPanelNotice.setVisibility(View.VISIBLE);
		}
		else {
			mPanelNotice.setVisibility(View.GONE);
		}

		mSwipeRefreshLayout.setRefreshing(data.isRunning());
	}

	/**
	 * @return "2 hours ago | Vnexpress > Duong Phu Hiep"
	 */
	private String getBasicInfo(Article article) {
		if (article == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(StrUtils.getTimeAgo(getResources(), article.getPublishedDateString()));
		sb.append(" | ");

		sb.append(getArticleSourceName(article));

		if (!Strings.isNullOrEmpty(article.getAuthor())) {
			sb.append(" &gt; " + article.getAuthor());
		}
		return sb.toString();
	}

	private String getTagsInfo(Subscription subscription) {
		if (subscription == null) {
			return null;
		}
		return Joiner.on(", ").join(Splitter.on('|').omitEmptyStrings()
				.split(subscription.getTags().toLowerCase()));
	}

	private String getArticleSourceName(Article article) {
		try {
			String hostName = (new URL(article.getArticleUrl())).getHost();

			//remove prefix "www."
			if (hostName.startsWith("www.")) {
				hostName = hostName.substring(4, hostName.length());
			}

			//transform to a link to the original article
			return String.format("<a href=\"%s\">%s</a>", article.getArticleUrl(), hostName);

		} catch (MalformedURLException e) {
			Log.w(TAG, e);
		}
		return getResources().getString(R.string.SOURCE_UNKNOWN);
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
