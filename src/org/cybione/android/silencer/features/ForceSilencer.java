package org.cybione.android.silencer.features;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

public class ForceSilencer {
	private static final String TAG = "Silencer";

	private Context mContext;
	private Handler mHandler;

	private int mRingerMode;

	public ForceSilencer(Context context) {
		mContext = context;
		mRingerMode = getRingerMode();
		mContext.getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
		mHandler = new Handler();
	}

	private int getRingerMode() {
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getRingerMode();
	}

	private void setRingerMode(int mode) {
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setRingerMode(mode);
	}

	private int getVolume() {
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getStreamVolume(AudioManager.STREAM_RING);
	}

	private void setVolume(int volume) {
		AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
	}

	private Runnable mRingerChangeRunnable = new Runnable() {
		@Override
		public void run() {
			if (mRingerMode != AudioManager.RINGER_MODE_VIBRATE) {
				if (getVolume() < 7) {
					Log.d(TAG, "forcing ringer mode vibrate");
					setVolume(0);
					setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				} else
					Log.d(TAG, "volume set to " + getVolume() + ", ignoring vibrate mode");
			}
		}
	};

	private ContentObserver mSettingsContentObserver = new ContentObserver(new Handler()) {
		@Override
		public synchronized void onChange(boolean selfChange) {
			int ringerMode = getRingerMode();
			if (ringerMode != mRingerMode) {
				Log.d(TAG, "ringer mode changed to " + ringerMode);
				mRingerMode = ringerMode;
				mHandler.removeCallbacks(mRingerChangeRunnable);
				mHandler.postDelayed(mRingerChangeRunnable, 100);
			}
		}
	};
}
