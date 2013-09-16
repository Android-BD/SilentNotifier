package org.cybione.android.silentnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SilentNotifier silentNotifier = SilentNotifier.getInstance();
		if (silentNotifier != null)
			silentNotifier.onAlarm();
	}

}
