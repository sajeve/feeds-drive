//package dh.newspaper.adapter;
//
//import android.view.View;
//import android.widget.Adapter;
//import dh.newspaper.base.DatabaseActivity;
//import dh.newspaper.model.FeedItem;
//import dh.newspaper.model.generated.DaoMaster;
//import dh.newspaper.model.generated.DaoSession;
//import org.lucasr.smoothie.SimpleItemLoader;
//
///**
// * Created by hiep on 26/05/2014.
// */
//public class FeedItemLoader extends SimpleItemLoader<String, FeedItem> {
//
//	private final DatabaseActivity mContext;
//	private final DaoMaster mDaoMaster;
//	private final DaoSession mDaoSession;
//
//	public FeedItemLoader(DatabaseActivity context) {
//		this.mContext = context;
//
//		mDaoMaster = new DaoMaster(context.getDatabase());
//		mDaoSession = mDaoMaster.newSession();
///*
//		try {
//			Article article = new Article(null, "articleUrl1", "parentUrl1", "imageUrl1", "title1", "author1", "excerpt1", "content1", "fa1256", "en", 0L, null, null, null, null, new Date());
//			mDaoSession.getArticleDao().insert(article);
//		} catch (Exception ex) {
//			Log.w(TAG, ex.getMessage());
//		}*/
//	}
//
//	@Override
//	public FeedItem loadItem(String itemParams) {
//		return new FeedItem("Lorem "+itemParams,"10:12:22","Loremep sdpsd oez", "https://vnexpress.net/rss/Haha", "vn");
//	}
//
//	@Override
//	public FeedItem loadItemFromMemory(String itemParams) {
//		return new FeedItem("Lorem Memory "+itemParams,"10:12:22","Loremep sdpsd oez", "https://vnexpress.net/rss/Haha", "vn");
//	}
//
//	@Override
//	public void displayItem(View itemView, FeedItem feedItem, boolean fromMemory) {
//
//	}
//
//	@Override
//	public String getItemParams(Adapter adapter, int position) {
//		//(AsyncBaseAdapter)adapter
//		return null;
//	}
//}
