package dh.newspaper.adapter;

import android.content.Context;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.cache.RefData;
import dh.newspaper.event.SubscribeClickedEvent;
import dh.newspaper.model.CheckableString;
import dh.newspaper.model.json.SearchFeedsResult;

import javax.inject.Inject;
import java.util.List;

/**
 * The tag list, with last element is "Add new.."
* Created by hiep on 8/05/2014.
*/
public class TagListSelectorAdapter extends BaseAdapter {
	private static final String TAG = TagListSelectorAdapter.class.getName();

	private final Context context;
	private final LayoutInflater inflater;
	private Handler mainThreadHandler;
	List<CheckableString> data;

	public TagListSelectorAdapter(Context context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		mainThreadHandler = new Handler();
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(final int position) {
		return data.get(position);
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
			boolean isSpecialItem = itemData instanceof CheckableString;
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
		return isSpecialItem ?
				inflater.inflate(R.layout.item_tag_new, parent, false)
				: inflater.inflate(R.layout.item_tag_checkable, parent, false);
	}

	public List<CheckableString> getData() {
		return data;
	}

	public void setData(List<CheckableString> data) {
		this.data = data;
		notifyDataSetChanged();
	}
}