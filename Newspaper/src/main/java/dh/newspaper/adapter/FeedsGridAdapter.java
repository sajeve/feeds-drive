package dh.newspaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.event.BaseEvent;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.model.FeedItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hiep on 8/05/2014.
 */
public class FeedsGridAdapter extends ArrayAdapter<FeedItem> {
	private static final String TAG = FeedsGridAdapter.class.getName();
	private ExecutorService mExecutor;

	private final LayoutInflater mInflater;
	private ContentParser mContentParser;
	private String mSourceAddress;

	private FeedsGridAdapter(Context context, int resource) {
		super(context, resource);
		mInflater = LayoutInflater.from(context);
	}

	public FeedsGridAdapter(Context context, ContentParser contentParser) {
		this(context, R.layout.item_feed);
		mContentParser = contentParser;
	}

	public void fetchAddress(String url) {
		if (!Strings.isNullOrEmpty(url) && mSourceAddress != url) {
			if (mExecutor != null) {
				mExecutor.shutdownNow();
			}
			mExecutor = Executors.newSingleThreadExecutor();
			mSourceAddress = url;
			mExecutor.execute(mGetDataFunc);
		}
	}

	public String getSourceAddress() {
		return mSourceAddress;
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
				v = mInflater.inflate(R.layout.item_feed, parent, false);
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

			FeedItem item = this.getItem(position);
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

	/**
	 * Parse rss items from mSourceAddress
	 */
	private Runnable mGetDataFunc = new Runnable() {
		@Override
		public void run() {
			try {
				try {
					//connect to the URL and fetch rss items
					final List<FeedItem> rssItems = mContentParser.parseRssUrl(mSourceAddress, "UTF-8");

					//notify GUI
					EventBus.getDefault().post(new Event() {{ data = rssItems; }});
				} catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
			catch (Exception ex) {
				Log.w(TAG, ex);
			}
		}
	};

	public class Event extends BaseEvent<FeedsGridAdapter> {
		public List<FeedItem> data;
		public Event() {
			super(FeedsGridAdapter.this);
		}
		public Event(String subject) {
			super(FeedsGridAdapter.this, subject);
		}
	}
}
