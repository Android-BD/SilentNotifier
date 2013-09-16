package org.cybione.android.silentnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BackgroundServiceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
				intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
			if (!BackgroundService.isStarted())
				context.startService(new Intent(context, BackgroundService.class));
		}
	}

}
