package org.cybione.android.silentnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IntentReceiver extends BroadcastReceiver {

	static public final String INTENT_ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE";
	static public final String INTENT_TELEPHONY_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {
		//android.util.Log.d("IntentReceiver", "Intent " + intent.getAction() + " received");

		SilentNotifier silentNotifier = SilentNotifier.getInstance();
		if (silentNotifier == null)
			return;

		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			silentNotifier.onScreenOn();
		if (intent.getAction().equals(INTENT_ACTION_PHONE_STATE))
			silentNotifier.onPhoneState();
		if (intent.getAction().equals(INTENT_TELEPHONY_SMS_RECEIVED))
			silentNotifier.onSMSReceived();
	}
}
