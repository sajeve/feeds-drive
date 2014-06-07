package dh.newspaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.greenrobot.dao.query.LazyList;
import dh.newspaper.R;
import dh.newspaper.model.generated.Article;
import dh.newspaper.tools.StrUtils;
import org.joda.time.DateTime;

/**
* Created by hiep on 8/05/2014.
*/
public class ArticlesGridAdapter extends BaseAdapter {
	private static final String TAG = ArticlesGridAdapter.class.getName();

	private final Context mContext;
	private final LayoutInflater mInflater;
	private int mNumberOfColumns;
	private LazyList<Article> mData;
	private int mCount;

	public ArticlesGridAdapter(Context context, int numberOfColumns) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mNumberOfColumns = numberOfColumns;
	}

	private int getItemResource() {
		if (mNumberOfColumns == 1) {
			return R.layout.item_feed_list;
		}
		return R.layout.item_feed_grid;
	}

	@Override
	public int getCount() {
		if (mData == null || mData.isClosed()) {
			return 0;
		}
		return mCount;
	}

	@Override
	public Object getItem(int position) {
		if (mData == null || mData.isClosed()) {
			return null;
		}
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		if (mData == null || mData.isClosed()) {
			return 0;
		}
		Article article = mData.get(position);
		if (article == null) {
			return  0;
		}
		return article.getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */

			View v;
			ImageView imageView;
			TextView titleLabel;
			TextView dateLabel;
			TextView excerptLabel;

			if (convertView == null) {
				// create new view
				v = mInflater.inflate(getItemResource(), parent, false);
				imageView = (ImageView) v.findViewById(R.id.article_image);
				titleLabel = (TextView) v.findViewById(R.id.article_title);
				dateLabel = (TextView) v.findViewById(R.id.article_date);
				excerptLabel = (TextView) v.findViewById(R.id.article_excerpt);
				v.setTag(new View[]{imageView, titleLabel, dateLabel, excerptLabel});
			} else {
				v = convertView;
				View[] viewsHolder = (View[]) v.getTag();
				imageView = (ImageView) viewsHolder[0];
				titleLabel = (TextView) viewsHolder[1];
				dateLabel = (TextView) viewsHolder[2];
				excerptLabel = (TextView) viewsHolder[3];
			}

			/* bind value to view */

			Article article = (Article)this.getItem(position);
			if (article != null) {
				titleLabel.setText(article.getTitle());
				dateLabel.setText(StrUtils.getTimeAgo(mContext.getResources(), article.getPublishedDateString()));
				excerptLabel.setText(article.getExcerpt());
			}
			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	public LazyList<Article> getData() {
		return mData;
	}

	public void setData(LazyList<Article> data, int count) {
		this.mData = data;
		this.mCount = count;
	}

	/*
	public class Event extends BaseEvent<ArticlesGridAdapter> {
		public List<Article> mData;
		public Event() {
			super(ArticlesGridAdapter.this);
		}
		public Event(String subject) {
			super(ArticlesGridAdapter.this, subject);
		}
	}*/
}