package org.cybione.android.silencer;

import org.cybione.android.silencer.features.ForceSilencer;
import org.cybione.android.silencer.features.VibrationNotifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

@SuppressLint("Wakelock")
public class MainService {
	public static final String TAG = "MainSerivec";

	private static MainService mInstance = null;
	private static Object mLock = new Object();
	private static WakeLock mWakeLock;

	private VibrationNotifier mVibrationNotifier;

	public MainService(Context context) {
		mInstance = this;

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				context.getResources().getText(R.string.app_name).toString());
	
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(IntentReceiver.INTENT_ACTION_PHONE_STATE);
		filter.addAction(IntentReceiver.INTENT_TELEPHONY_SMS_RECEIVED);
        context.registerReceiver(new IntentReceiver(), filter);

		mVibrationNotifier = new VibrationNotifier(context);
		new ForceSilencer(context);
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
		synchronized (mLock) {
			mWakeLock.acquire();
			mInstance = null;
			mWakeLock.release();
		}
	}
}
