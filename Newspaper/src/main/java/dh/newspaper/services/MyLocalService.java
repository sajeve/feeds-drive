package dh.newspaper.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by hiep on 30/05/2014.
 */
public class MyLocalService extends Service {
	private static final String TAG=MyLocalService.class.getName();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand "+intent.getAction() + "startId="+startId);

		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
