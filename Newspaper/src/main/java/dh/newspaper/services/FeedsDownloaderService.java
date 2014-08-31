package dh.newspaper.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import dh.newspaper.Constants;
import dh.newspaper.MainActivity;
import dh.newspaper.R;
import dh.newspaper.base.Injector;
import dh.newspaper.cache.RefData;
import dh.newspaper.tools.NetworkUtils;
import dh.tool.thread.prifo.IQueueEmptyCallback;
import dh.tool.thread.prifo.PrifoExecutor;
import dh.tool.thread.prifo.PrifoExecutorFactory;
import dh.newspaper.workflow.SelectTagWorkflow;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by hiep on 30/05/2014.
 */
public class FeedsDownloaderService extends Service {
	private static final String TAG = FeedsDownloaderService.class.getName();
	private static final SimpleDateFormat TimeFormat = new SimpleDateFormat("H:mm");
	private static final int WaitLastTaskFinishDuration = 10000;

	@Inject RefData mRefData;
	@Inject SharedPreferences mPreferences;
	@Inject ConnectivityManager mConnectivityManager;
	private PrifoExecutor mArticlesLoader;
	private PrifoExecutor mSelectTagLoader;

	@Override
	public void onCreate() {
		((Injector)getApplication()).getObjectGraph().inject(this);
		Log.i(TAG, "onCreate Service");

		mArticlesLoader = mRefData.createArticleLoader();
		mSelectTagLoader = PrifoExecutorFactory.newPrifoExecutor("TagLoader", 1);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//testService(startId);
		Log.i(TAG, "onStartCommand " + (intent==null ? "" : intent.getAction()) + " startId=" + startId);

		//check service enabled
		if (!mRefData.getPreferenceServiceEnabled()) {
			Log.i(TAG, "Service is disabled");
			if (Constants.DEBUG) {
				displayNotification("Articles receiving is not happen", "Service is disabled");
			}
			return Service.START_FLAG_REDELIVERY;
		}

		//check connectivity
		if (!NetworkUtils.networkConditionMatched(mConnectivityManager, mPreferences)) {
			Log.i(TAG, "Network not available");
			if (Constants.DEBUG) {
				displayNotification("Articles receiving is not happen", "Network is not available");
			}
			return Service.START_FLAG_REDELIVERY;
		}

		//check charging condition
		if (mRefData.getPreferenceOnlyRunServiceIfCharging()) {
			if (!mRefData.isBatteryCharging()) {
				Log.i(TAG, "Device is not charging");
				if (Constants.DEBUG) {
					displayNotification("Articles receiving is not happen", "Device is not charging");
				}
				return Service.START_FLAG_REDELIVERY;
			}
		}

		mRefData.updateArticleLoaderPoolSize(mArticlesLoader);

		//main action
		downloadAll();

		return Service.START_FLAG_REDELIVERY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/*private void testService(int startId) {
		Log.i("ALARM", "Service is called: "+startId);
		Toast.makeText(this, "Service is called: "+startId, Toast.LENGTH_SHORT).show();
	}*/

	List<SelectTagWorkflow> selectTagWorkflowList;

	public void downloadAll() {
		new AsyncTask<Object, Object, Boolean>() {
			@Override
			protected Boolean doInBackground(Object[] params) {
				try {
					mRefData.getLruDiscCache(); //setupLruDiscCache
					mRefData.loadSubscriptionAndTags();
					return true;
				}
				catch (Exception ex) {
					Log.w(TAG, ex);
					displayNotification("Failed download articles", ex.toString());
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
					displayNotificationOnMainThread("Start download articles", "Start download all articles from " + mRefData.getActiveTags().size() + " tags");

					//compute article TTL base on the service interval
					Duration articlesTTL = new Duration(mRefData.getPreferenceServiceInterval());

					selectTagWorkflowList = new ArrayList<SelectTagWorkflow>();

					for (String tag : mRefData.getActiveTags()) {
						SelectTagWorkflow selectTagWorkflow = new SelectTagWorkflow(getApplicationContext(), tag,
								Constants.SUBSCRIPTION_TTL_MIN, articlesTTL, true, Constants.ARTICLES_PER_PAGE,
								mArticlesLoader, null);
						selectTagWorkflowList.add(selectTagWorkflow);
						mSelectTagLoader.execute(selectTagWorkflow);
					}

					mSelectTagLoader.setQueueEmptyCallback(tagQueueEmptyCallback);
					mArticlesLoader.setQueueEmptyCallback(articleQueueEmptyCallback);
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


	private final IQueueEmptyCallback tagQueueEmptyCallback = new IQueueEmptyCallback() {
		@Override
		public void onQueueEmpty(String queueName) {
			//the queue is empty, but the last item is still processing
			//we will wait 10s and hope that the last one will be finished
			// then check if all the pending task was complete
			Handler mainThread = new Handler(Looper.getMainLooper());
			mainThread.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						if (selectTagWorkflowList == null || selectTagWorkflowList.size() == 0) {
							Log.w(TAG, "IllegalStateException: Tag queue empty, but the task list is null");
							return;
						}

						Calendar startTime = null;
						Calendar endTime = null;
						for (SelectTagWorkflow wf : selectTagWorkflowList) {
							Calendar s = wf.getStartTime();
							Calendar e = wf.getEndTime();
							if (s == null || e == null) {
								Log.i(TAG, "Tag queue empty, but a workflow is still processing: "+wf);
								return; //not finish all the workflow
							}
							if (startTime == null || startTime.after(s)) {
								startTime = s;
							}
							if (endTime == null || endTime.before(e)) {
								endTime = e;
							}
						}

						Duration duration = new Duration(new DateTime(startTime), new DateTime(endTime));
						displayNotification("Download tags finished", String.format("Downloaded %d tags at %s in %d sec",
								selectTagWorkflowList.size(),
								TimeFormat.format(startTime.getTime()),
								duration.getStandardSeconds()
								));
					}
					catch (Exception ex) {
						Log.w(TAG, ex);
					}
				}
			}, WaitLastTaskFinishDuration);
		}
	};
	private final IQueueEmptyCallback articleQueueEmptyCallback = new IQueueEmptyCallback() {
		@Override
		public void onQueueEmpty(String queueName) {
			//the queue is empty, but the last item is still processing
			//we will wait 10s and hope that the last one will be finished
			// then check if all the pending task was complete
			Handler mainThread = new Handler(Looper.getMainLooper());
			mainThread.postDelayed(new Runnable() {
				@Override
				public void run() {
					try {
						if (selectTagWorkflowList == null || selectTagWorkflowList.size() == 0) {
							Log.w(TAG, "IllegalStateException: Article queue empty, but the task list is null");
							return;
						}

						Calendar startTime = null;
						Calendar endTime = null;
						int countDownloadedArticles = 0;
						int wfLeft = 0;
						for (SelectTagWorkflow wf : selectTagWorkflowList) {
							Calendar s = wf.getStartTime();
							Calendar e = wf.getEndTimeAll();
							if (s == null || e == null) { //workflow was in progress
								Log.i(TAG, "Tag queue empty, but a workflow is still processing: "+wf);
								wfLeft++;
							}
							else { //workflow is complete
								if (startTime == null || startTime.after(s)) {
									startTime = s;
								}
								if (endTime == null || endTime.before(e)) {
									endTime = e;
								}

								int d = wf.countArticlesToDownload();
								if (d > 0) {
									countDownloadedArticles += d;
								}
							}
						}

						if (wfLeft == 0) {
							Duration duration = new Duration(new DateTime(startTime), new DateTime(endTime));
							displayNotification("Download all articles finished", String.format("Downloaded %d articles at %s in %d sec",
									countDownloadedArticles,
									TimeFormat.format(startTime.getTime()),
									duration.getStandardSeconds()
							));
						}
						else if (countDownloadedArticles > 0) {
							displayNotification("Download articles in progress",
									String.format("%d articles downloaded. %d/%d tags left to download", countDownloadedArticles, wfLeft, selectTagWorkflowList.size()));
						}
					}
					catch (Exception ex) {
						Log.w(TAG, ex);
					}
				}
			}, WaitLastTaskFinishDuration);

			//displayNotification("Download articles", "Articles queue is empty");
		}
	};

	public void displayNotification(final String title, final String text) {
		try {
			//Log.w(TAG, message, ex);
			if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
				displayNotificationOnMainThread(title, text);
			}
			else {
				Handler mainThread = new Handler(Looper.getMainLooper());
				mainThread.post(new Runnable() {
					@Override
					public void run() {
						try {
							displayNotificationOnMainThread(title, text);
						}
						catch (Exception ex1) {
							Log.wtf(TAG, ex1);
						}
					}
				});
			}
		}
		catch (Exception ex2) {
			Log.wtf(TAG, ex2);
		}
	}

	public void displayNotificationOnMainThread(String title, String text) {
		try {
			//text = DateTime.now().toString("HH:mm")+" "+text;
			Log.i(TAG, title+": "+text);

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
		catch (Exception ex) {
			Log.wtf(TAG, ex);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (selectTagWorkflowList!=null) {
			for (SelectTagWorkflow wf : selectTagWorkflowList) {
				wf.cancel();
			}
		}
		mArticlesLoader.cancelAll();
		mSelectTagLoader.cancelAll();
	}

	//	public void downloadAllTest() {
//		new AsyncTask<Object, Object, Boolean>() {
//			@Override
//			protected Boolean doInBackground(Object[] params) {
//				try {
//					mRefData.getLruDiscCache(); //setupLruDiscCache
//					mRefData.loadSubscriptionAndTags();
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
//					Log.i(TAG, "Alarm Fired, nbOfTags = " + mRefData.getActiveTags().size());
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
