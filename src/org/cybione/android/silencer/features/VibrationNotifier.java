package org.cybione.android.silencer.features;

import org.cybione.android.silencer.AlarmReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

public class VibrationNotifier {
	private static final String TAG = "VibrationNotifier";

	private Context mContext;

	private PowerManager mPowerManager;
	private AudioManager mAudioManager;
	private AlarmManager mAlarmManager;
	private PendingIntent mAlarmPendingIntent = null;
	private Vibrator mVibrator;

	private MissedCall mMissedCall;
	private UnreadSMS mUnreadSMS;

	private long mNotifierStartTime;

	private static final int SEC = 1000;
	private static final int MIN = 60 * SEC;

	private static final int STEP_CHECK[] =		{ 30 * SEC,		2 * MIN,	-1 };
	private static final int STEP_DURATION[] =	{ 5 * MIN,		30 * MIN,	-1 };

	private static final long[] VIBRATION_PATTERN = { 0, 500, 200, 500, 200, 500 };

	public VibrationNotifier(Context context) {
		mContext = context;
		mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

		mMissedCall = new MissedCall(mContext);
		mUnreadSMS = new UnreadSMS(mContext);

		mNotifierStartTime = 0;

		programAlarm(STEP_CHECK[0]);
	}

	public boolean check() {
		if (!mPowerManager.isScreenOn()) {
			// ignore if in call
			if (mAudioManager.getMode() == AudioManager.MODE_IN_CALL)
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

	public void programAlarm(long delay) {
		cancelAlarm();
		Intent intent = new Intent(mContext, AlarmReceiver.class);
		mAlarmPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
		mAlarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, mAlarmPendingIntent);
	}

	public void cancelAlarm() {
		if (mAlarmPendingIntent != null) {
			mAlarmManager.cancel(mAlarmPendingIntent);
			mAlarmPendingIntent = null;
		}
	}

	public void onScreenOn() {
		if (mNotifierStartTime > 0) {
			Log.d(TAG, "Screen turned on, disabling notifier.");
			mNotifierStartTime = 0;
			cancelAlarm();
		}
	}

	public void onPhoneState() {
		Log.d(TAG, "Phone state changed, will check for missing call...");
		if (mNotifierStartTime == 0)
			programAlarm(STEP_CHECK[0]);
	}

	public void onSMSReceived() {
		Log.d(TAG, "SMS received, will check for unread SMS...");
		if (mNotifierStartTime == 0)
			programAlarm(STEP_CHECK[0]);
	}

	private int getCurrentStep() {
		if (mNotifierStartTime == 0)
			return -1;
		long now = System.currentTimeMillis();
		long duration = 0;
		for (int i = 0; i < STEP_DURATION.length; ++i) {
			duration += STEP_DURATION[i];
			if ((now - mNotifierStartTime) < duration)
				return i;
		}
		return -1;
	}

	public void onAlarm() {
		if (mNotifierStartTime > 0) {
			int step = getCurrentStep();
			if (step >= 0) {
				Log.d(TAG, "Notify (step " + step + ")");
				mVibrator.vibrate(VIBRATION_PATTERN, -1);
				programAlarm(STEP_CHECK[step]);
			}
		} else if (check()) {
			mNotifierStartTime = System.currentTimeMillis();
			programAlarm(0);
		}
	}

}
