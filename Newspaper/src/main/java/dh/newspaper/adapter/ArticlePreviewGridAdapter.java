package dh.newspaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dh.newspaper.R;
import dh.newspaper.parser.RssItem;

import java.util.List;

/**
 * Created by hiep on 8/05/2014.
 */
public class ArticlePreviewGridAdapter extends ArrayAdapter<RssItem> {
	private static final String TAG = ArticlePreviewGridAdapter.class.getName();

	private final LayoutInflater inflater;


	public ArticlePreviewGridAdapter(Context context, int resource) {
		super(context, resource);
		inflater = LayoutInflater.from(context);
	}

	public ArticlePreviewGridAdapter(Context context, int resource, List<RssItem> objects) {
		super(context, resource, objects);
		inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */

			View v;
			ImageView imageView;
			TextView titleLabel;
			TextView dateLabel;

			if (convertView == null) {
				// create new view
				v = inflater.inflate(R.layout.article_preview, parent, false);
				imageView = (ImageView) v.findViewById(R.id.article_image);
				titleLabel = (TextView) v.findViewById(R.id.article_title);
				dateLabel = (TextView) v.findViewById(R.id.article_date);
				v.setTag(new View[]{imageView, titleLabel, dateLabel});
			} else {
				v = convertView;
				View[] viewsHolder = (View[]) v.getTag();
				imageView = (ImageView) viewsHolder[0];
				titleLabel = (TextView) viewsHolder[1];
				dateLabel = (TextView) viewsHolder[2];
			}

			/* bind value to view */

			RssItem item = this.getItem(position);
			if (item != null) {
				titleLabel.setText(item.getTitle());
				dateLabel.setText(item.getPublishedDate());
			}

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

}
