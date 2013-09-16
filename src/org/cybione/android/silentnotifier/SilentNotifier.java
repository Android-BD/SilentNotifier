package org.cybione.android.silentnotifier;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.util.Log;

public class SilentNotifier {
	public static final String TAG = "SilentNotifier";

	private static SilentNotifier mInstance = null;
	private Context mContext;
	private PowerManager mPowerManager;
	private AlarmManager mAlarmManager;
	private PendingIntent mAlarmPendingIntent = null;
	private Object mLock = new Object();

	private Vibrator mVibrator;
	private ButtonsLed mButtonLed;
	private MissedCall mMissedCall;
	private UnreadSMS mUnreadSMS;

	private int mNotifierStep = 0;
	private Thread mNotifierThread = null;
	private WakeLock mNotifierWakeLock;

	private static final int CHECK_TIME_AFTER_CALL = 30 * 1000;
	private static final int CHECK_TIME_AFTER_SMS = 30 * 1000;

	// step 1: notify every 8 seconds (vibrate every 32 seconds) during 5 minutes
	private static final int NOTIFICATION_STEP1_DELAY = 8 * 1000;
	private static final int NOTIFICATION_STEP1_STOP = (5 * 60 * 1000) / NOTIFICATION_STEP1_DELAY;
	private static final int NOTIFICATION_STEP1_VIBRATOR = (32 * 1000) / NOTIFICATION_STEP1_DELAY;

	// step 2: notify every 16 seconds (vibrate every 2 minutes) during 10 minutes
	private static final int NOTIFICATION_STEP2_DELAY = 16 * 1000;
	private static final int NOTIFICATION_STEP2_STOP = (10 * 60 * 1000) / NOTIFICATION_STEP2_DELAY + NOTIFICATION_STEP1_STOP;
	private static final int NOTIFICATION_STEP2_VIBRATOR = (2 * 60 * 1000) / NOTIFICATION_STEP2_DELAY;

	// step 3: notify every 10 seconds
	private static final int NOTIFICATION_STEP3_DELAY = 16 * 1000;

	private static final int NOTIFICATION_REPEAT = 3;
	private static final int NOTIFICATION_INTERVAL_ON = 500;
	private static final int NOTIFICATION_INTERVAL_OFF = 500;

	public SilentNotifier(Context context) {
		mContext = context;
		mInstance = this;

		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mNotifierWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "_notifier");

		mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		mButtonLed = new ButtonsLed();
		mMissedCall = new MissedCall(context);
		mUnreadSMS = new UnreadSMS(context);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(IntentReceiver.INTENT_ACTION_PHONE_STATE);
		filter.addAction(IntentReceiver.INTENT_TELEPHONY_SMS_RECEIVED);
        context.registerReceiver(new IntentReceiver(), filter);

		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		programAction(CHECK_TIME_AFTER_CALL);

		mButtonLed.off();
	}

	public static SilentNotifier getInstance() {
		return mInstance;
	}

	public boolean check() {
		if (!mPowerManager.isScreenOn()) {
			// ignore if in call
			AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getMode() == AudioManager.MODE_IN_CALL)
					return false;

			if (mMissedCall.check()) {
				Log.i(TAG, "Missed call");
				return true;
			}
			if (mUnreadSMS.check()) {
				Log.i(TAG, "Unread SMS");
				return true;
			}
		}
		return false;
	}

	public void programAction(long delay) {
		cancelAction();
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		mAlarmPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, mAlarmPendingIntent);
	}

	public void cancelAction() {
		if (mAlarmPendingIntent != null) {
			mAlarmManager.cancel(mAlarmPendingIntent);
			mAlarmPendingIntent = null;
		}
	}

	private void startNotifier() {
		if (mNotifierStep > 0)
			return;
		Log.d(TAG, "Starting notification");
		mNotifierStep++;
		mNotifierWakeLock.acquire();
		runNotifier();
	}

	private void stopNotifier() {
		if (mNotifierStep > 0) {
			Log.d(TAG, "Stopping notification");
			mNotifierStep = 0;
		}
		if (mNotifierThread != null) {
			mNotifierThread.interrupt();
			try {
				mNotifierThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (mNotifierWakeLock.isHeld())
			mNotifierWakeLock.release();
		cancelAction();
		mButtonLed.off();
	}

	private class NotifierRunnable implements Runnable {
		@Override
		public void run() {
			runNotifier();
		}
	}

	@SuppressLint("Wakelock")
	private void runNotifier() {
		if (mNotifierStep == 0)
			return;
		if (!mNotifierWakeLock.isHeld())
			mNotifierWakeLock.acquire();
		int delay = 0, time;

		if (mNotifierStep < NOTIFICATION_STEP1_STOP) {
			if (mNotifierStep % NOTIFICATION_STEP1_VIBRATOR == 1)
				time = notificationBlinkAndVibrate();
			else
				time = notificationBlink();
			delay = NOTIFICATION_STEP1_DELAY - time;
		} else if (mNotifierStep < NOTIFICATION_STEP2_STOP) {
			if ((mNotifierStep - NOTIFICATION_STEP1_STOP) % NOTIFICATION_STEP2_VIBRATOR == 0)
				time = notificationBlinkAndVibrate();
			else
				time = notificationBlink();
			delay = NOTIFICATION_STEP2_DELAY;
		} else {
			time = notificationBlink();
			delay = NOTIFICATION_STEP3_DELAY - time;
		}

		synchronized (mLock) {
			if (mNotifierStep > 0) {
				if (mNotifierStep < NOTIFICATION_STEP2_STOP)
					++mNotifierStep;
				if (delay > 0)
					programAction(delay);	
			}
		}

		if (mNotifierWakeLock.isHeld())
			mNotifierWakeLock.release();
	}

	private int notificationBlinkAndVibrate() {
		Log.d(TAG, "Notification with vibration");
		int time = 0;
		for (int i = 0; i < NOTIFICATION_REPEAT && mNotifierStep > 0; ++i) {
			mButtonLed.on();
			mVibrator.vibrate(NOTIFICATION_INTERVAL_ON);
			try {
				Thread.sleep(NOTIFICATION_INTERVAL_ON);
			} catch (InterruptedException e) {
				mVibrator.cancel();
				mButtonLed.off();
				return time;
			}
			time += NOTIFICATION_INTERVAL_ON;
			mVibrator.cancel();
			mButtonLed.off();
			try {
				Thread.sleep(NOTIFICATION_INTERVAL_OFF);
			} catch (InterruptedException e) {
				return time;
			}
			time += NOTIFICATION_INTERVAL_OFF;
		}
		return time;
	}

	private int notificationBlink() {
		Log.d(TAG, "Notification");
		int time = 0;
		for (int i = 0; i < NOTIFICATION_REPEAT && mNotifierStep > 0; ++i) {
			mButtonLed.on();
			try {
				Thread.sleep(NOTIFICATION_INTERVAL_ON);
			} catch (InterruptedException e) {
				mButtonLed.off();
				return time;
			}
			time += NOTIFICATION_INTERVAL_ON;
			mButtonLed.off();
			try {
				Thread.sleep(NOTIFICATION_INTERVAL_OFF);
			} catch (InterruptedException e) {
				return time;
			}
			time += NOTIFICATION_INTERVAL_OFF;
		}
		return time;
	}

	public void onScreenOn() {
		synchronized (mLock) {
			stopNotifier();
		}
	}

	public void onPhoneState() {
		synchronized (mLock) {
			if (mNotifierStep == 0) {
				Log.d(TAG, "Phone state changed, will check for missing call in " + CHECK_TIME_AFTER_CALL + " ms");
				programAction(CHECK_TIME_AFTER_CALL);
			}
		}
	}

	public void onSMSReceived() {
		synchronized (mLock) {
			if (mNotifierStep == 0) {
				Log.d(TAG, "SMS received, will check for unread SMS in " + CHECK_TIME_AFTER_SMS + " ms");
				programAction(CHECK_TIME_AFTER_SMS);
			}
		}
	}

	public void onAlarm() {
		synchronized (mLock) {
			mAlarmPendingIntent = null;
			if (mNotifierStep > 0) {
				mNotifierWakeLock.acquire();
				mNotifierThread = new Thread(new NotifierRunnable());
				mNotifierThread.start();
			} else if (check())
				startNotifier();
		}
	}

	public void onDestroy() {
		synchronized (mLock) {
			stopNotifier();
		}
		mInstance = null;
	}
}
