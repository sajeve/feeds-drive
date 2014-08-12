package dh.newspaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import dh.newspaper.R;
import dh.newspaper.model.CheckableString;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * The tag list, with last element is "Add new.."
* Created by hiep on 8/05/2014.
*/
public class TagListSelectorAdapter extends BaseAdapter {
	private static final String TAG = TagListSelectorAdapter.class.getName();

	private final Context context;
	private final LayoutInflater inflater;
	//private Handler mainThreadHandler;
	private ArrayList<CheckableString> data;
	private final View.OnClickListener itemOnClick;

	public TagListSelectorAdapter(Context context, View.OnClickListener itemOnClick) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.itemOnClick = itemOnClick;
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
			if (convertView == null) {
				v = inflater.inflate(R.layout.item_tag_checkable, parent, false);
				v.setOnClickListener(this.itemOnClick);
			}
			else {
				v = convertView;
			}

			/* bind value to view */
			((CheckedTextView)v).setText(itemData.getText());
			((ListView)parent).setItemChecked(position, itemData.isChecked());
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

	public ArrayList<CheckableString> getData() {
		return data;
	}

	public void setData(ArrayList<CheckableString> data) {
		this.data = data;
		notifyDataSetChanged();
	}

	public Set<String> getSelectedTags() {
		if (data==null) {
			return null;
		}
		Set<String> ret = new TreeSet<String>();
		for (CheckableString cs : data) {
			if (cs.isChecked()) {
				ret.add(cs.getText());
			}
		}
		return ret;
	}
}