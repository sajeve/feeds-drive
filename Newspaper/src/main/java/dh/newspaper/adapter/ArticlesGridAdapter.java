package dh.newspaper.adapter;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.common.base.Strings;
import com.nostra13.universalimageloader.core.ImageLoader;
import dh.newspaper.Constants;
import dh.newspaper.R;
import dh.newspaper.model.generated.Article;
import dh.newspaper.tools.DateUtils;
import dh.newspaper.workflow.SelectTagWorkflow;
import dh.tool.common.StrUtils;

/**
* Created by hiep on 8/05/2014.
*/
public class ArticlesGridAdapter extends BaseAdapter {
	private static final String TAG = ArticlesGridAdapter.class.getName();

	private final Context mContext;
	private final LayoutInflater mInflater;
	private int mNumberOfColumns;
	private IArticleCollection mData;
	private Handler mMainThreadHandler;

	public ArticlesGridAdapter(Context context, int numberOfColumns) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mNumberOfColumns = numberOfColumns;
		mMainThreadHandler = new Handler();
	}

	private int getItemResource() {
		if (mNumberOfColumns == 1) {
			return R.layout.item_feed_list;
		}
		return R.layout.item_feed_grid;
	}

	@Override
	public int getCount() {
		if (mData == null) {
			return 0;
		}
		return mData.getTotalSize();
	}

	private Runnable lastGetItemCall;
	@Override
	public Object getItem(final int position) {
		if (mData == null) {
			return null;
		}
		try {
			/*if (position==0) {
				Log.v(TAG, "position 0");
				//return null;
			}*/
			if (mData.isInMemoryCache(position)) {
				return mData.getArticle(position);
			}
			else {
				if (lastGetItemCall!=null) {
					mMainThreadHandler.removeCallbacks(lastGetItemCall); //we received new call, so remove the last one
				}

				lastGetItemCall = new Runnable() {
					@Override
					public void run() {
						try {
							AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
								@Override
								protected Void doInBackground(Void... params) {
									mData.getArticle(position);
									return null;
								}

								@Override
								protected void onPostExecute(Void aVoid) {
									notifyDataSetChanged();
								}
							};

							task.execute();
						}
						catch (Exception ex) {
							Log.w(TAG, ex);
						}
					}
				};

				mMainThreadHandler.postDelayed(lastGetItemCall, Constants.EVENT_DELAYED);
				return null;
			}
		}
		catch (Exception ex) {
			Log.w(TAG, ex);
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		Article article = (Article)getItem(position);
		if (article == null) {
			return 0;
		}
		return article.getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */

			ViewSwitcher v;
			ImageView imageView;
			TextView titleLabel;
			TextView dateLabel;
			TextView excerptLabel;
			TextView sourceLabel;
			TextView lastUpdateLabel;

			if (convertView == null) {
				// create new view
				v = (ViewSwitcher)mInflater.inflate(getItemResource(), parent, false);
				imageView = (ImageView) v.findViewById(R.id.article_image);
				titleLabel = (TextView) v.findViewById(R.id.article_title);
				dateLabel = (TextView) v.findViewById(R.id.article_date);
				excerptLabel = (TextView) v.findViewById(R.id.article_excerpt);
				sourceLabel = (TextView) v.findViewById(R.id.source);
				lastUpdateLabel = (TextView) v.findViewById(R.id.last_update);
				v.setTag(new View[]{imageView, titleLabel, dateLabel, excerptLabel, sourceLabel, lastUpdateLabel});
			} else {
				v = (ViewSwitcher)convertView;
				View[] viewsHolder = (View[]) v.getTag();
				imageView = (ImageView) viewsHolder[0];
				titleLabel = (TextView) viewsHolder[1];
				dateLabel = (TextView) viewsHolder[2];
				excerptLabel = (TextView) viewsHolder[3];
				sourceLabel = (TextView) viewsHolder[4];
				lastUpdateLabel = (TextView) viewsHolder[5];
			}

			/* bind value to view */

			Article article = (Article)this.getItem(position);
			if (article != null) {
				String publishDate = DateUtils.getTimeAgo(mContext.getResources(), article.getPublishedDate());
				if (TextUtils.isEmpty(publishDate)) {
					publishDate = article.getPublishedDateString();
				}
				/*if (Constants.DEBUG) {
					publishDate += " | "+StrUtils.domainName(article.getArticleUrl())+" | "+ DateUtils.getTimeAgo(mContext.getResources(), article.getLastDownloadSuccess());
				}*/
				titleLabel.setText(article.getTitle());
				dateLabel.setText(publishDate);
				excerptLabel.setText(article.getExcerpt());

				//imageView.setVisibility(View.VISIBLE);
				String imageUrl = article.getImageUrl();
				imageView.setImageResource(R.drawable.card_background_gray);
				if (!Strings.isNullOrEmpty(imageUrl)) {
					ImageLoader.getInstance().displayImage(imageUrl, imageView);
				}
				sourceLabel.setText(StrUtils.domainName(article.getArticleUrl()));
				lastUpdateLabel.setText(DateUtils.getTimeAgo(mContext.getResources(), article.getLastDownloadSuccess()));
			}
			/*else {
				imageView.setVisibility(View.INVISIBLE);
			}*/

			switchView(v, article==null);

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	private void switchView(ViewSwitcher v, boolean toProgressBar) {
		if ((v.getNextView() instanceof ProgressBar && toProgressBar) ||
				(! (v.getNextView() instanceof ProgressBar) && !toProgressBar)) {
			v.showNext();
		}
		return;
	}

	public IArticleCollection getData() {
		return mData;
	}

	public void setData(IArticleCollection data) {
		Log.d(TAG, "setData(" + data + ") count=" + data.getTotalSize());
		this.mData = data;
		//this.mData.setCacheChangeListener(cacheChangedListener);
		notifyDataSetChanged();
	}

/*	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();

		if (mData!=null)
			Log.d(TAG, String.format("notifyDataSetChanged '%s' N=%d",
				((SelectTagWorkflow)mData).getTag(),
				mData.getTotalSize()));
	}*/

/*	private SelectTagWorkflow.OnInMemoryCacheChangeCallback cacheChangedListener = new IArticleCollection.OnInMemoryCacheChangeCallback() {
		@Override
		public void onChanged(IArticleCollection sender) {
			((Activity)mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					try {
						notifyDataSetChanged();
					} catch (Exception e) {
						Log.w(TAG, e);
					}
				}
			});
		}
	};*/

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