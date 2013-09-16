package org.cybione.android.silentnotifier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {
	private static final String TAG = "BackgroundService";

	private static SilentNotifier mService = null;

	public static boolean isStarted() {
		return mService != null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		try {
			if (mService == null) {
				Log.d(TAG, "Starting service");
				mService = new SilentNotifier(this);
			}
		} catch (Exception e) {
			Log.e(TAG, "Cannot start service: " + getResources().getText(R.string.app_name), e);
		}
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		synchronized (this) {
			mService.onDestroy();
			mService = null;
		}
		super.onDestroy();
	}

}
