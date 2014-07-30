package dh.newspaper.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import dh.newspaper.Constants;
import dh.newspaper.MyApplication;
import dh.newspaper.R;
import dh.newspaper.adapter.TagListSelectorAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.CreateNewTagEvent;
import dh.newspaper.event.SaveSubscriptionEvent;
import dh.newspaper.model.AddNewItem;
import dh.newspaper.model.CheckableString;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.services.BackgroundTasksManager;
import dh.newspaper.workflow.SaveSubscriptionWorkflow;
import dh.tool.common.StrUtils;

import javax.inject.Inject;
import java.util.*;

/**
 *
 */
public class SubscriptionDialog extends DialogFragment {
	private static final String TAG = SubscriptionDialog.class.getName();

    private static final String ARG_FEEDS_SOURCE_INFO = "feedsSource";
	private static final String ARG_TAGS_LIST_DATA = "tagsListData";

	private ListView tagList;
	private Button okButton;
	private Button cancelButton;
	private TagListSelectorAdapter tagsListAdapter;
	private ProgressDialog savingProgressDialog;

	@Inject RefData refData;
	@Inject BackgroundTasksManager backgroundTasksManager;
	private SearchFeedsResult.ResponseData.Entry feedsSource;
	private ArrayList<CheckableString> tagsListData;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SubscriptionDialog.
     */
    public static SubscriptionDialog newInstance(SearchFeedsResult.ResponseData.Entry feedsSource) {
        SubscriptionDialog fragment = new SubscriptionDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_FEEDS_SOURCE_INFO, feedsSource);
        fragment.setArguments(args);
        return fragment;
    }
    public SubscriptionDialog() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            feedsSource = (SearchFeedsResult.ResponseData.Entry)getArguments().getSerializable(ARG_FEEDS_SOURCE_INFO);
        }
		setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_tag_selector);
		getDialog().setCanceledOnTouchOutside(false);

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_subscription_dialog, container, false);
		tagList = (ListView)v.findViewById(R.id.tag_list);
		okButton = (Button)v.findViewById(R.id.ok_button);
		cancelButton = (Button)v.findViewById(R.id.cancel_button);

		{//update list view
			tagsListAdapter = new TagListSelectorAdapter(this.getActivity());
			tagList.setAdapter(tagsListAdapter);
		}
		{//update progress bar
			savingProgressDialog = new ProgressDialog(getActivity());
			savingProgressDialog.setCancelable(false);
			savingProgressDialog.setCanceledOnTouchOutside(false);
		}
		okButton.setOnClickListener(onOkClicked);
		cancelButton.setOnClickListener(onCancelClicked);

		return v;
    }

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);

		//restore tag list
		if (tagsListData==null) {
			if (feedsSource!=null) {
				tagsListData = getTagsListData(feedsSource);
			}
		}
		tagsListAdapter.setData(tagsListData);

		//restoreProgressState(); //if do it here, the progressDialog will go behind the current dialog
	}

	/*@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(ARG_FEEDS_SOURCE_INFO, feedsSource);
		if (tagsListAdapter!=null && tagsListAdapter.getData()!=null) {
			outState.putSerializable(ARG_TAGS_LIST_DATA, tagsListAdapter.getData());
		}
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) { return; }
		feedsSource = (SearchFeedsResult.ResponseData.Entry)savedInstanceState.getSerializable(ARG_FEEDS_SOURCE_INFO);
		ArrayList<CheckableString> tagsData = (ArrayList<CheckableString>) savedInstanceState.getSerializable(ARG_TAGS_LIST_DATA);
		if (tagsData!=null && tagsData.size()>0) {
			tagsListAdapter.setData(tagsData);
		}
		else {
			setGui(feedsSource);
		}

	*//*	if (backgroundTasksManager.getActiveSelectTagWorkflow()!=null) {
			setGui(backgroundTasksManager.getActiveSaveSubscriptionWorkflow().getSaveSubscriptionState());
		}*//*
	}*/

	private ArrayList<CheckableString> getTagsListData(SearchFeedsResult.ResponseData.Entry state) {
		//the feed sources is already subscribed with some tags
		HashSet<String> selectedTags = state==null || state.getSubscription()==null ? null :
				Sets.newHashSet(
						Splitter.on('|')
								.omitEmptyStrings()
								.split(state.getSubscription().getTags().toUpperCase())
				);

		ArrayList<CheckableString> checkableTags = new ArrayList<CheckableString>();
		for (String tagName : refData.getTags()) {
			checkableTags.add(new CheckableString(tagName, selectedTags != null && selectedTags.contains(tagName)));
		}
		checkableTags.add(new AddNewItem());
		return checkableTags;
	}

	private View.OnClickListener onOkClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				Set<String> selectedTags = tagsListAdapter.getSelectedTags();
				if (selectedTags==null || selectedTags.size()==0) {
					Crouton.makeText(getActivity(), R.string.select_at_least_one_category, Style.ALERT, (ViewGroup)getView()).show();
					/*Toast.makeText(SubscriptionDialog.this.getActivity(),
							R.string.select_at_least_one_category, Toast.LENGTH_SHORT).show();*/
					return;
				}

				savingProgressDialog.show(); //TODO translate
				backgroundTasksManager.saveSubscription(feedsSource, selectedTags);
			} catch (Exception ex) {
				Log.w(TAG, ex);
				MyApplication.showErrorDialog(getFragmentManager(), "OK Clicked", ex);
			}
		}
	};

	private View.OnClickListener onCancelClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			try {
				//SubscriptionDialog.this.onCancel(SubscriptionDialog.this.getDialog());
				SubscriptionDialog.this.dismiss();
			} catch (Exception ex) {
				Log.w(TAG, ex);
				MyApplication.showErrorDialog(getFragmentManager(), "Cancel Clicked", ex);
			}
		}
	};

	public void onEventMainThread(CreateNewTagEvent event) {
		try {
			//TODO
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(getFragmentManager(), event.toString(), ex);
		}
	}

	public void onEventMainThread(SaveSubscriptionEvent event) {
		try {
			if (feedsSource!=null && !StrUtils.equalsString(event.getFlowId(), feedsSource.getUrl())) {
				return; //event not concerned
			}

			if (StrUtils.equalsString(Constants.SUBJECT_SAVE_SUBSCRIPTION_PROGRESS_MESSAGE, event.getSubject())) {
				savingProgressDialog.setMessage(event.getProgressMessage());
				savingProgressDialog.show(); //TODO translate
			}
			else if (StrUtils.equalsString(Constants.SUBJECT_SAVE_SUBSCRIPTION_DONE, event.getSubject())) {
				savingProgressDialog.setMessage(event.getProgressMessage());
				savingProgressDialog.dismiss();
				dismiss();
			}
			else if (StrUtils.equalsString(Constants.SUBJECT_SAVE_SUBSCRIPTION_ERROR, event.getSubject())) {
				savingProgressDialog.dismiss();
				showErrorDialog("Failed saving subscription", event.getProgressMessage()); //TODO: translate
			}
		} catch (Exception ex) {
			Log.w(TAG, ex);
			MyApplication.showErrorDialog(getFragmentManager(), event.toString(), ex);
		}
	}

	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance())
			getDialog().setDismissMessage(null);

		savingProgressDialog.dismiss();

		super.onDestroyView();
	}

	private void restoreProgressState() {
		SaveSubscriptionWorkflow savingWorkflow = backgroundTasksManager.getActiveSaveSubscriptionWorkflow();
		if (savingWorkflow!=null && savingWorkflow.isRunning()) {
			savingProgressDialog.show();
			SaveSubscriptionEvent savingState = savingWorkflow.getSaveSubscriptionState();
			if (savingState!=null) {
				savingProgressDialog.setMessage(savingState.getProgressMessage());
			}
		}
	}

	public void showErrorDialog(final String title, final String message) {
		AlertDialog aDialog = new AlertDialog.Builder(getActivity()).setMessage(message).setTitle(title)
				.setNeutralButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
										final int which) {
						//Prevent to finish activity, if user clicks about.
						if (!title.equalsIgnoreCase("About") && !title.equalsIgnoreCase("Directory Error") && !title.equalsIgnoreCase("View")) {
							getActivity().finish();
						}

					}
				})
				//.setIcon(R.drawable.ic_dialog_alert)
				.create();
		aDialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault().register(this);
		restoreProgressState(); //restore here so that the progressDialog won't go behind the current dialog
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
}
