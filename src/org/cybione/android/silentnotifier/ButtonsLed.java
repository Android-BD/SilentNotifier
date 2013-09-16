package org.cybione.android.silentnotifier;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import android.util.Log;

public class ButtonsLed {
	private static final String TAG = SilentNotifier.TAG + ".ButtonsLed";

	private static final String BRIGHTNESS_FILE = "/sys/class/leds/button-backlight/brightness";
	private static final int BRIGHTNESS_ON = 255;
	private static final int BRIGHTNESS_OFF = 0;

	private static final String SU = "/system/xbin/su";

	private boolean mUseButtonLed = false;

	public ButtonsLed() {
		mUseButtonLed = fileExist(BRIGHTNESS_FILE) && fileExist(SU);
		if (!mUseButtonLed)
			Log.e(TAG, "Cannot use buttons led");
	}

	private boolean fileExist(String fileName) {
		File file = new File(fileName);
		return file.exists() && file.isFile();
	}

	private void setBrightness(int level) {
		try {
			Process p = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("echo " + String.valueOf(level) + " > /sys/class/leds/button-backlight/brightness\n");
			os.writeBytes("exit\n");
			os.close();
		} catch (IOException e) {
			Log.e(TAG, "Cannot use buttons led", e);
		}
	}

	public void on() {
		if (mUseButtonLed)
			setBrightness(BRIGHTNESS_ON);
	}

	public void off() {
		if (mUseButtonLed)
			setBrightness(BRIGHTNESS_OFF);
	}
}

