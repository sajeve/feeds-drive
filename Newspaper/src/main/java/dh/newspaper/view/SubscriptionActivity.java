package dh.newspaper.view;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.adapter.SearchFeedsResultAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.event.SearchFeedsEvent;
import dh.newspaper.event.SubscribeClickedEvent;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.workflow.SearchFeedsTask;
import dh.tool.common.StrUtils;

import javax.inject.Inject;

public class SubscriptionActivity extends Activity {
	private static final String TAG = SubscriptionActivity.class.getName();

	private EditText edtQuery;
	private SwipeRefreshLayout swipeRefresh;
	private ListView resultList;
	private TextView txtNotice;
	private View panelNotice;
	private SearchFeedsResultAdapter searchFeedsResultAdapter;

	@Inject BackgroundTasksManager mBackgroundTasksManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);
		((Injector)getApplication()).inject(this);

		getActionBar().setTitle(R.string.title_activity_subscription);
		edtQuery = (EditText)findViewById(R.id.query_editor);
		swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
		resultList = (ListView)findViewById(R.id.search_result);
		txtNotice = (TextView) findViewById(R.id.txt_notice);
		panelNotice = findViewById(R.id.panel_notice);

		searchFeedsResultAdapter = new SearchFeedsResultAdapter(this);
		resultList.setAdapter(searchFeedsResultAdapter);

		edtQuery.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				try {
					if (actionId == EditorInfo.IME_ACTION_SEARCH) {
						mBackgroundTasksManager.searchFeedsSources(v.getText().toString());

						//hide keyboard
						InputMethodManager imm= (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

						return true;
					}
				} catch (Exception ex) {
					Log.w(TAG, ex);
					MyApplication.showErrorDialog(SubscriptionActivity.this.getFragmentManager(), "IME_ACTION_SEARCH", ex);
				}
				return false;
			}
		});
		swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				try {
					if (lastTask!=null) {
						mBackgroundTasksManager.searchFeedsSources(lastTask.getMissionId());
					}
					else {
						if (Strings.isNullOrEmpty(edtQuery.getText().toString())) {
							swipeRefresh.setRefreshing(false);
						}
						else {
							mBackgroundTasksManager.searchFeedsSources(edtQuery.getText().toString());
						}
					}
				} catch (Exception ex) {
					Log.w(TAG, ex);
					MyApplication.showErrorDialog(SubscriptionActivity.this.getFragmentManager(), "Refresh", ex);
				}
			}
		});

		lastTask = mBackgroundTasksManager.getActiveSearchFeedsTask();
		setGui(lastTask);
    }

	private SearchFeedsTask lastTask;

	public void onEventMainThread(SearchFeedsEvent event) {
		try {
			if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_SEARCH_FEEDS_START_LOADING)) {
				lastTask = (SearchFeedsTask)event.getSender();
				swipeRefresh.setRefreshing(true);
			}
			else if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_SEARCH_FEEDS_REFRESH)) {
				if (lastTask !=null && !StrUtils.equalsString(lastTask.getMissionId(), event.getFlowId())) {
					//this event is fired by a sender which is no more concerning by this activity -> do nothing
					return;
				}
				setGui(lastTask);
			}
			else if (StrUtils.equalsString(event.getSubject(), Constants.SUBJECT_SEARCH_FEEDS_DONE_LOADING)) {
				if (lastTask !=null && !StrUtils.equalsString(lastTask.getMissionId(), event.getFlowId())) {
					//this event is fired by a sender which is no more concerning by this activity -> do nothing
					return;
				}
				setGui(lastTask);
			}
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	public void onEventMainThread(SubscribeClickedEvent event) {
		try {
			//Toast.makeText(this, event.getFeedsSourceUrl(), Toast.LENGTH_LONG).show();
			SubscriptionDialog subDlg = SubscriptionDialog.newInstance(event.getFeedsSource());
			subDlg.show(getFragmentManager(), SubscriptionDialog.class.getName());
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(this.getFragmentManager(), event.getSubject(), ex);
		}
	}

	private void setGui(SearchFeedsTask currentTask) {
		if (currentTask == null) {return;}

		swipeRefresh.setRefreshing(!currentTask.isDone() || currentTask.isCancelled());

		SearchFeedsEvent event = currentTask.getSearchResultEvent();
		if (event==null) { return; }

		if (event.getSearchResult()!=null) {
			searchFeedsResultAdapter.setData(event.getSearchResult());
			resultList.setVisibility(View.VISIBLE);
			panelNotice.setVisibility(View.GONE);
		}
		else if (event.getError()!=null) {
			txtNotice.setText(String.format(getResources().getString(R.string.SEARCH_FAILED), currentTask.getQuery(), event.getError().toString()));
			txtNotice.setError(Log.getStackTraceString(event.getError()));
			resultList.setVisibility(View.GONE);
			panelNotice.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		lastTask = mBackgroundTasksManager.getActiveSearchFeedsTask();
		setGui(lastTask);
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
}
