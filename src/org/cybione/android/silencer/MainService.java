package org.cybione.android.silencer;

import org.cybione.android.silencer.features.ForceSilencer;
import org.cybione.android.silencer.features.VibrationNotifier;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

@SuppressLint("Wakelock")
public class MainService {
	public static final String TAG = "MainService";

	private static MainService mInstance = null;

	private Context mContext;
	private Object mLock = new Object();
	private WakeLock mWakeLock;
	private BroadcastReceiver mIntentReceiver;
	private VibrationNotifier mVibrationNotifier;

	public MainService(Context context) {
		mInstance = this;
		mContext = context;

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				mContext.getResources().getText(R.string.app_name).toString());
	
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(IntentReceiver.INTENT_ACTION_PHONE_STATE);
		filter.addAction(IntentReceiver.INTENT_TELEPHONY_SMS_RECEIVED);
		mIntentReceiver = new IntentReceiver();
        mContext.registerReceiver(mIntentReceiver, filter);

		mVibrationNotifier = new VibrationNotifier(context);
		new ForceSilencer(mContext);
	}

	public static MainService getInstance() {
		return mInstance;
	}

	public void onScreenOn() {
		synchronized (mLock) {
			mWakeLock.acquire();
			mVibrationNotifier.onScreenOn();
			mWakeLock.release();
		}
	}

	public void onPhoneState() {
		synchronized (mLock) {
			mWakeLock.acquire();
			mVibrationNotifier.onPhoneState();
			mWakeLock.release();
		}
	}

	public void onSMSReceived() {
		synchronized (mLock) {
			mWakeLock.acquire();
			mVibrationNotifier.onSMSReceived();
			mWakeLock.release();
		}
	}

	public void onAlarm() {
		synchronized (mLock) {
			mWakeLock.acquire();
			mVibrationNotifier.onAlarm();
			mWakeLock.release();
		}
	}

	public void onDestroy() {
		mInstance = null;
		mContext.unregisterReceiver(mIntentReceiver);
	}
}
