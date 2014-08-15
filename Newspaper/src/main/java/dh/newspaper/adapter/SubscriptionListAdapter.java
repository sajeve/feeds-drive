package dh.newspaper.adapter;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import dh.newspaper.R;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;
import dh.newspaper.tools.TagUtils;
import dh.newspaper.view.utils.BgCheckbox;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
* Created by hiep on 8/05/2014.
*/
public class SubscriptionListAdapter extends BaseAdapter {
	private static final String TAG = SubscriptionListAdapter.class.getName();

	private final Context mContext;
	private final LayoutInflater mInflater;
	private List<Subscription> subscriptions;
	private final View.OnClickListener onClickSubscribe;
	private final BgCheckbox.ICheckedAction onChangeChecked;
	private final Executor executor;
	//private Handler mMainThreadHandler;

	public SubscriptionListAdapter(Context context, Executor executor, View.OnClickListener onClickSubscribe, BgCheckbox.ICheckedAction onChangeChecked) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		this.executor = executor;
		this.onClickSubscribe = onClickSubscribe;
		this.onChangeChecked = onChangeChecked;
	}

	@Override
	public int getCount() {
		if (subscriptions == null) {
			return 0;
		}
		return subscriptions.size();
	}

	@Override
	public Object getItem(final int position) {
		if (subscriptions == null) {
			return null;
		}
		return subscriptions.get(position);
	}

	@Override
	public long getItemId(int position) {
		Subscription sub = (Subscription)getItem(position);
		if (sub==null) {
			return 0;
		}
		else {
			return sub.getId();
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */
			View v;
			BgCheckbox checkBox;
			TextView textUrl;
			TextView category;
			ImageButton editBtn;

			if (convertView == null) {
				// create new view
				v = mInflater.inflate(R.layout.item_subscription, parent, false);
				checkBox = (BgCheckbox) v.findViewById(R.id.bgCheckbox);
				textUrl = (TextView) v.findViewById(R.id.url);
				category = (TextView) v.findViewById(R.id.categories);
				editBtn = (ImageButton) v.findViewById(R.id.edit_category);
				v.setTag(new View[]{checkBox, textUrl, category, editBtn});
				//title.setMovementMethod(LinkMovementMethod.getInstance());
				editBtn.setOnClickListener(onClickSubscribe);
				checkBox.setOnCheckedChangeListener(onChangeChecked);
				checkBox.setExecutor(executor);
			} else {
				v = convertView;
				View[] viewsHolder = (View[]) v.getTag();
				checkBox = (BgCheckbox) viewsHolder[0];
				textUrl = (TextView) viewsHolder[1];
				category = (TextView) viewsHolder[2];
				editBtn = (ImageButton) viewsHolder[3];
			}

			/* bind value to view */

			Subscription sub = (Subscription)this.getItem(position);
			if (sub != null) {
				checkBox.setChecked(sub.getEnable());
				textUrl.setText(sub.getFeedsUrl());
				category.setText(TagUtils.getPrintableLowerCasesTags(sub.getTags()));
				editBtn.setTag(sub);
				checkBox.setTag(sub);
				textUrl.setTextAppearance(mContext, sub.getEnable() ? R.style.ArticleLink : R.style.ArticleLink_GrayHint_Italic);
				category.setTextAppearance(mContext, sub.getEnable() ? R.style.Article_Info_Gray : R.style.Article_Info_GrayHint);
			}

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	public void setData(List<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
		notifyDataSetChanged();
	}
}