package dh.newspaper.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoExecutorFactory;
import dh.newspaper.workflow.SelectTagWorkflow;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

/**
 * Created by hiep on 30/05/2014.
 */
public class FeedsDownloaderService extends Service {
	private static final String TAG = FeedsDownloaderService.class.getName();

	@Override
	public void onCreate() {
		((Injector)getApplication()).getObjectGraph().inject(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand " + (intent==null ? "" : intent.getAction()) + " startId=" + startId);

		//Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
		downloadAll();

		return Service.START_FLAG_REDELIVERY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Inject RefData mRefData;
	private PrifoExecutor mArticlesLoader = PrifoExecutorFactory.newPrifoExecutor(1, Constants.THREAD_ARTICLES_LOADER);
	private PrifoExecutor mSelectTagLoader = PrifoExecutorFactory.newPrifoExecutor(1, 2);

	public void downloadAll() {
		new AsyncTask<Object, Object, Boolean>() {
			@Override
			protected Boolean doInBackground(Object[] params) {
				try {
					mRefData.getLruDiscCache(); //setupLruDiscCache
					mRefData.loadTags();
					return true;
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				try {
					if (!result) {
						Log.w(TAG, "stop process service");
						return;
					}
					mRefData.initImageLoader();

					Log.i(TAG, "Start download all "+mRefData.getTags().size()+ " tags");
					displayNotification("Start download articles", "Start download all articles from "+mRefData.getTags().size()+ " tags");

					for (String tag : mRefData.getTags()) {
						SelectTagWorkflow selectTagWorkflow = new SelectTagWorkflow(getApplicationContext(), tag,
								Constants.SUBSCRIPTION_TTL, Constants.ARTICLE_TTL_SERVICE, false, Constants.ARTICLES_PER_PAGE,
								mArticlesLoader, null);

						mSelectTagLoader.execute(selectTagWorkflow);
					}
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
		}.execute();

//		Executors.newSingleThreadExecutor(PriorityThreadFactory.MIN).execute(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					mArticlesLoader.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
//				}
//				catch (Exception ex) {
//					Log.w(TAG, ex);
//				}
//			}
//		});
	}

	public void displayNotification(String title, String text) {
		Intent resultIntent = new Intent(this, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the started Activity.
		// This ensures that navigating backward from the Activity leads out of your application to the Home screen.

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);

		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);


		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.ic_launcher)
						.setContentTitle(title)
						.setContentText(text);
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// id = 0 allows you to update the notification later on.
		mNotificationManager.notify(0, mBuilder.build());
	}

//	public void downloadAllTest() {
//		new AsyncTask<Object, Object, Boolean>() {
//			@Override
//			protected Boolean doInBackground(Object[] params) {
//				try {
//					mRefData.getLruDiscCache(); //setupLruDiscCache
//					mRefData.loadTags();
//					return true;
//				}
//				catch (Exception ex) {
//					Log.w(TAG, ex);
//					return false;
//				}
//			}
//
//			@Override
//			protected void onPostExecute(Boolean result) {
//				try {
//					if (!result) {
//						Log.w(TAG, "stop process service");
//						return;
//					}
//					mRefData.initImageLoader();
//
//					Log.i(TAG, "Alarm Fired, nbOfTags = " + mRefData.getTags().size());
//				} catch (Exception ex) {
//					Log.w(TAG, ex);
//				}
//			}
//		}.execute();
//
//		/*Toast toast = Toast.makeText(getApplicationContext(), "Download feeds alarm fired", Toast.LENGTH_SHORT);
//		toast.show();*/
//	}
}
