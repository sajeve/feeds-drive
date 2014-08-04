package dh.newspaper.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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
import dh.newspaper.event.RefreshArticleEvent;
import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.tools.DateUtils;
import dh.tool.common.StrUtils;
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

		mTxtDataSource.setMovementMethod(LinkMovementMethod.getInstance());

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				try {
					refreshGUI(true);
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
		refreshGUI(false);
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

	private static final String STATE_ARTICLE = "Article";

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(STATE_ARTICLE, mArticle);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		mArticle = (Article)savedInstanceState.getSerializable(STATE_ARTICLE);
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
		if (!isAdded() || mWebView==null) {
			return;
		}
		try {
			if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_ARTICLE_START_LOADING)) {
				swRae = Stopwatch.createStarted();
				Log.d(TAG, "Received "+event);

				mSwipeRefreshLayout.setRefreshing(true);
				setGui(event.getSender());

				return;
			}

			if (mArticle != null && !StrUtils.equalsString(mArticle.getArticleUrl(), event.getSender().getArticleUrl())) {
				Log.d(TAG, "ignore "+event+" because currentArticleUrl="+mArticle.getArticleUrl());
				//this event is fired by a sender which is no more concerning by this fragment -> do nothing
				return;
			}

			if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_ARTICLE_REFRESH)) {
				Log.d(TAG, "ArticleFragment REFRESH (" + swRae.elapsed(TimeUnit.MILLISECONDS) + " ms) " + event.getSender().getArticleUrl());
				swRae.reset().start();

				setGui(event.getSender());
				return;
			}
			if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_ARTICLE_DONE_LOADING)) {
				Log.d(TAG, "ArticleFragment DONE (" + swRae.elapsed(TimeUnit.MILLISECONDS) + " ms) " + event.getSender().getArticleUrl());

				mSwipeRefreshLayout.setRefreshing(false);
				return;
			}
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	/**
	 * if forced = true, then force to run Article loading workflow
	 * otherwise, just refresh the UI base on the current state
	 * @param forced
	 */
	private void refreshGUI(boolean forced) {
		SelectArticleWorkflow selectArticleWorkflow = mBackgroundTasksManager.getActiveSelectArticleWorkflow();

		if (forced || selectArticleWorkflow == null) {
			mBackgroundTasksManager.loadArticle(mArticle);
			//GUI will be refreshed via EventBus
		}
		else {
			//refresh GUI base on current state
			setGui(selectArticleWorkflow);
		}
	}
	private void setGui(SelectArticleWorkflow data) {
		if (data == null) {
			Log.w(TAG, "data is null");
			if (Constants.DEBUG) {
				throw new IllegalStateException("data is null");
			}
		}

		mSwipeRefreshLayout.setRefreshing(data.isRunning());

		mArticle = data.getArticle();

		if (mArticle == null) {
			Log.i(TAG, "Article is null");
			return;
		}

		mTxtTitle.setText(mArticle.getTitle());
		mTxtTags.setText(getTagsInfo(data.getParentSubscription()));
		mTxtDataSource.setText(Html.fromHtml(getBasicInfo(mArticle)));

		String encoding=data.getParentSubscription() == null ? null : data.getParentSubscription().getEncoding();
		mWebView.loadDataWithBaseURL(mArticle.getArticleUrl(), mArticle.getContent(), "text/html", encoding, mArticle.getParentUrl());

		if (!Strings.isNullOrEmpty(mArticle.getParseNotice())) {
			mTxtNotice.setText(mArticle.getParseNotice() + " - " + mArticle.getArticleUrl());
			mPanelNotice.setVisibility(View.VISIBLE);
		}
		else {
			mPanelNotice.setVisibility(View.GONE);
		}
	}

	/**
	 * @return "2 hours ago | Vnexpress > Duong Phu Hiep"
	 */
	private String getBasicInfo(Article article) {
		if (article == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(DateUtils.getTimeAgo(getResources(), article.getPublishedDateString()));
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
		if (TextUtils.isEmpty(subscription.getTags())) {
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


/*
	public class Event extends BaseEventOneArg<ArticleFragment> {
		public FeedItem rssItem;
		public Event() {
			super(ArticleFragment.this);
		}
		public Event(String subject) {
			super(ArticleFragment.this, subject);
		}
	}
*/
}
