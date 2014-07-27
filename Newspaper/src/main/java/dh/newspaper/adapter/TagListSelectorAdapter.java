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
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SubscribeClickedEvent;
import dh.newspaper.model.AddNewItem;
import dh.newspaper.model.CheckableString;
import dh.newspaper.model.json.SearchFeedsResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * The tag list, with last element is "Add new.."
* Created by hiep on 8/05/2014.
*/
public class TagListSelectorAdapter extends BaseAdapter {
	private static final String TAG = TagListSelectorAdapter.class.getName();

	private final Context context;
	private final LayoutInflater inflater;
	//private Handler mainThreadHandler;
	private List<CheckableString> data;

	public TagListSelectorAdapter(Context context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		//mainThreadHandler = new Handler();
	}

	@Override
	public int getCount() {
		return data==null ? 0 : data.size();
	}

	@Override
	public Object getItem(final int position) {
		return data==null ? null : data.get(position);
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
			CheckableString itemData = (CheckableString)getItem(position);
			boolean isSpecialItem = itemData instanceof AddNewItem;
			if (convertView == null) {
				v = createViewItem(parent, isSpecialItem);
			} else {
				/*if item and convert view mismatch, recreate the view, otherwise, use convert view*/
				v = isSpecialItem == isSpecialView(convertView) ? convertView : createViewItem(parent, isSpecialItem);
			}

			/* bind value to view */
			if (!isSpecialItem) {
				((CheckedTextView)v).setText(itemData.getText());
				((CheckedTextView)v).setChecked(itemData.isChecked());
			}
			v.setTag(itemData);

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	/**
	 * Detect if a item view is a special one (the last one)
	 */
	private boolean isSpecialView(View v) {
		//the last item is a normal TextView, not a CheckedTextView
		return !(v instanceof CheckedTextView);
	}

	private View createViewItem(ViewGroup parent, boolean isSpecialItem) {
		View v = isSpecialItem ?
				inflater.inflate(R.layout.item_tag_new, parent, false)
				: inflater.inflate(R.layout.item_tag_checkable, parent, false);

		//if user click on item: reverse the selection and update the view
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckableString itemData = (CheckableString)v.getTag();
				boolean isSpecialItem = itemData instanceof AddNewItem;
				if (isSpecialItem) {
					//TODO send event to add category
					Toast.makeText(context, "Add category", Toast.LENGTH_SHORT).show();
					//EventBus.getDefault().post( );
				}
				else {
					//reverse the selection
					itemData.setChecked(!itemData.isChecked());
					//update the view
					((CheckedTextView)v).setChecked(itemData.isChecked());
				}
			}
		});
		return v;
	}

	public List<CheckableString> getData() {
		return data;
	}

	public void setData(List<CheckableString> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	public List<String> getSelectedTags() {
		if (data==null) {
			return null;
		}
		List<String> ret = new ArrayList<String>();
		for (CheckableString cs : data) {
			if (cs.isChecked()) {
				ret.add(cs.getText());
			}
		}
		return ret;
	}
}