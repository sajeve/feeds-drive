package dh.newspaper.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.adapter.TagListSelectorAdapter;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.CreateNewTagEvent;
import dh.newspaper.event.SubscribeClickedEvent;
import dh.newspaper.model.AddNewItem;
import dh.newspaper.model.CheckableString;
import dh.newspaper.model.json.SearchFeedsResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class SubscriptionDialog extends DialogFragment {
    private static final String ARG_STATE = "state";
    private SearchFeedsResult.ResponseData.Entry state;

	private ListView tagList;
	private Button okButton;
	private Button cancelButton;
	private TagListSelectorAdapter tagListAdapter;

	@Inject RefData refData;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SubscriptionDialog.
     */
    public static SubscriptionDialog newInstance(SearchFeedsResult.ResponseData.Entry state) {
        SubscriptionDialog fragment = new SubscriptionDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_STATE, state);
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
            state = (SearchFeedsResult.ResponseData.Entry)getArguments().getSerializable(ARG_STATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
		getDialog().setTitle(R.string.title_tag_selector);

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_subscription_dialog, container, false);
		tagList = (ListView)v.findViewById(R.id.tag_list);
		okButton = (Button)v.findViewById(R.id.ok_button);
		cancelButton = (Button)v.findViewById(R.id.cancel_button);
		tagListAdapter = new TagListSelectorAdapter(this.getActivity());
		tagList.setAdapter(tagListAdapter);

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				List<String> selectedTags = tagListAdapter.getSelectedTags();
				if (selectedTags!=null) {
					Toast.makeText(SubscriptionDialog.this.getActivity(),
							Arrays.toString(selectedTags.toArray()), Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(SubscriptionDialog.this.getActivity(),
							"No data", Toast.LENGTH_SHORT).show();
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(SubscriptionDialog.this.getActivity(),
						"Cancel", Toast.LENGTH_SHORT).show();
			}
		});

		setGui(state);
		return v;
    }

	public void setGui(SearchFeedsResult.ResponseData.Entry state) {
		//the feed sources is already subscribed with some tags
		HashSet<String> selectedTags = state==null || state.getSubscription()==null ? null :
				Sets.newHashSet(
						Splitter.on('|')
								.omitEmptyStrings()
								.split(state.getSubscription().getTags().toUpperCase())
				);

		List<CheckableString> checkableTags = new ArrayList<CheckableString>();
		for (String tagName : refData.getTags()) {
			checkableTags.add(new CheckableString(tagName, selectedTags != null && selectedTags.contains(tagName)));
		}
		checkableTags.add(new AddNewItem());

		tagListAdapter.setData(checkableTags);
	}

	public void onEventMainThread(CreateNewTagEvent event) {

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(ARG_STATE, state);
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState == null) { return; }
		state = (SearchFeedsResult.ResponseData.Entry)savedInstanceState.getSerializable(ARG_STATE);
		setGui(state);
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
		//mArticle = mAppBundle.getCurrentArticle();
	}
}
