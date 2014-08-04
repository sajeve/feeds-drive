package dh.newspaper.adapter;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.event.SubscribeClickedEvent;
import dh.newspaper.model.generated.Subscription;
import dh.newspaper.model.json.SearchFeedsResult;

import java.util.Objects;

/**
* Created by hiep on 8/05/2014.
*/
public class SearchFeedsResultAdapter extends BaseAdapter {
	private static final String TAG = SearchFeedsResultAdapter.class.getName();

	private final Context mContext;
	private final LayoutInflater mInflater;
	private SearchFeedsResult mData;
	//private Handler mMainThreadHandler;

	public SearchFeedsResultAdapter(Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		//mMainThreadHandler = new Handler();
	}

	@Override
	public int getCount() {
		if (mData == null) {
			return 0;
		}
		return mData.count();
	}

	@Override
	public Object getItem(final int position) {
		if (mData == null) {
			return null;
		}
		return mData.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */
			View v;
			TextView title;
			TextView description;
			TextView source;
			ImageButton subscribe;

			if (convertView == null) {
				// create new view
				v = mInflater.inflate(R.layout.item_search_result, parent, false);
				title = (TextView) v.findViewById(R.id.title);
				description = (TextView) v.findViewById(R.id.description);
				source = (TextView) v.findViewById(R.id.source);
				subscribe = (ImageButton) v.findViewById(R.id.subscribe);
				v.setTag(new View[]{title, description, source, subscribe});

				title.setMovementMethod(LinkMovementMethod.getInstance());
				subscribe.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							Object[] dataHolder = (Object[]) v.getTag();
							if (dataHolder!=null) {
								SearchFeedsResult.ResponseData.Entry entry = (SearchFeedsResult.ResponseData.Entry)dataHolder[0];
								EventBus.getDefault().post(new SubscribeClickedEvent(SubscribeClickedEvent.SUBJECT_CLICK, entry));
							}
						}
						catch (Exception ex) {
							Log.w(TAG, ex);
						}
					}
				});
			} else {
				v = convertView;
				View[] viewsHolder = (View[]) v.getTag();
				title = (TextView) viewsHolder[0];
				description = (TextView) viewsHolder[1];
				source = (TextView) viewsHolder[2];
				subscribe = (ImageButton) viewsHolder[3];
			}

			/* bind value to view */

			SearchFeedsResult.ResponseData.Entry itemData = (SearchFeedsResult.ResponseData.Entry)this.getItem(position);
			if (itemData != null) {
				title.setLinkTextColor(1234);
				title.setText(Html.fromHtml(String.format("<font color=\"black\"><a href=\"%s\"=>%s</a></font>", itemData.getLink(), itemData.getTitle())));
				description.setText(Html.fromHtml(itemData.getContentSnippet()));
				source.setText(itemData.getUrl());

				Object[] dataHolder = (Object[])subscribe.getTag();
				Subscription currentHoldingSubscription = dataHolder==null ? null : (Subscription)dataHolder[1];

				//display other icon (checked) if the URL had been already subscribed
				if ((currentHoldingSubscription==null) != (itemData.getSubscription()==null)) {
					int res = itemData.getSubscription()==null ? R.drawable.add_259b24ff : R.drawable.checked_circle_72d572ff;
					subscribe.setImageDrawable(mContext.getResources().getDrawable(res));
				}

				//the saveSubscription button hold all the information
				subscribe.setTag(new Object[] {itemData, itemData.getSubscription()});
			}

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	public SearchFeedsResult getData() {
		return mData;
	}

	public void setData(SearchFeedsResult data) {
		this.mData = data;
		notifyDataSetChanged();
	}
}