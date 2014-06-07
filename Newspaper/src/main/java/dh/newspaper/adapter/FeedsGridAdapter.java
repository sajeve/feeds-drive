//package dh.newspaper.adapter;
//
//import android.content.Context;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ImageView;
//import android.widget.TextView;
//import com.google.common.base.Strings;
//import de.greenrobot.event.EventBus;
//import dh.newspaper.R;
//import dh.newspaper.event.BaseEvent;
//import dh.newspaper.model.FeedItem;
//import dh.newspaper.parser.ContentParser;
//import dh.newspaper.tools.StrUtils;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
///**
// * Created by hiep on 8/05/2014.
// */
//public class ArticlesGridAdapter extends ArrayAdapter<FeedItem> {
//	private static final String TAG = ArticlesGridAdapter.class.getName();
//	private ExecutorService mExecutor;
//
//	private final LayoutInflater mInflater;
//	private ContentParser mContentParser;
//	private String mSourceAddress;
//	private int mNumberOfColumns;
//
//	private ArticlesGridAdapter(Context context, int resource) {
//		super(context, resource);
//		mInflater = LayoutInflater.from(context);
//	}
//
//	public ArticlesGridAdapter(Context context, ContentParser contentParser, int numberOfColumns) {
//		this(context, R.layout.item_feed_grid);
//		mContentParser = contentParser;
//		mNumberOfColumns = numberOfColumns;
//	}
//
//	public void fetchAddress(String url) {
//		if (!Strings.isNullOrEmpty(url) && mSourceAddress != url) {
//			if (mExecutor != null) {
//				mExecutor.shutdownNow();
//			}
//			mExecutor = Executors.newSingleThreadExecutor();
//			mSourceAddress = url;
//			mExecutor.execute(mGetDataFunc);
//		}
//	}
//
//	public String getSourceAddress() {
//		return mSourceAddress;
//	}
//
//	/*private int getNumberOfColumn() {
//		if (this.getContext() instanceof DetailActivity) {
//			return 1;
//		}
//		return getContext().getResources().getInteger(R.integer.grid_columns_count);
//	}*/
//
//	private int getItemResource() {
//		if (mNumberOfColumns == 1) {
//			return R.layout.item_feed_list;
//		}
//		return R.layout.item_feed_grid;
//	}
//
//	@Override
//	public View getView(int position, View convertView, ViewGroup parent) {
//		try {
//			/* create (or get) view */
//
//			View v;
//			ImageView imageView;
//			TextView titleLabel;
//			TextView dateLabel;
//			TextView excerptLabel;
//
//			if (convertView == null) {
//				// create new view
//				v = mInflater.inflate(getItemResource(), parent, false);
//				imageView = (ImageView) v.findViewById(R.id.article_image);
//				titleLabel = (TextView) v.findViewById(R.id.article_title);
//				dateLabel = (TextView) v.findViewById(R.id.article_date);
//				excerptLabel = (TextView) v.findViewById(R.id.article_excerpt);
//				v.setTag(new View[]{imageView, titleLabel, dateLabel, excerptLabel});
//			} else {
//				v = convertView;
//				View[] viewsHolder = (View[]) v.getTag();
//				imageView = (ImageView) viewsHolder[0];
//				titleLabel = (TextView) viewsHolder[1];
//				dateLabel = (TextView) viewsHolder[2];
//				excerptLabel = (TextView) viewsHolder[3];
//			}
//
//			/* bind value to view */
//
//			FeedItem item = this.getItem(position);
//			if (item != null) {
//				titleLabel.setText(item.getTitle());
//				dateLabel.setText(StrUtils.getTimeAgo(this.getContext(), item.getPublishedDate()));
//				excerptLabel.setText(item.getDescription());
//			}
//
//			return v;
//		} catch (Exception ex) {
//			Log.w(TAG, ex);
//			return null;
//		}
//	}
//
//	/**
//	 * Parse rss items from mSourceAddress
//	 */
//	private Runnable mGetDataFunc = new Runnable() {
//		@Override
//		public void run() {
//			try {
//				try {
//					//connect to the URL and fetch rss items
//					final List<FeedItem> rssItems = mContentParser.parseFeeds(mSourceAddress, "UTF-8");
//
//					//notify GUI
//					EventBus.getDefault().post(new Event() {{ mData = rssItems; }});
//				} catch (Exception ex) {
//					Log.w(TAG, ex);
//				}
//			}
//			catch (Exception ex) {
//				Log.w(TAG, ex);
//			}
//		}
//	};
//
//	public class Event extends BaseEvent<ArticlesGridAdapter> {
//		public List<FeedItem> mData;
//		public Event() {
//			super(ArticlesGridAdapter.this);
//		}
//		public Event(String subject) {
//			super(ArticlesGridAdapter.this, subject);
//		}
//	}
//}
