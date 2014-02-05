package org.cybione.android.silencer.features;

import org.cybione.android.silencer.MainService;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.util.Log;

public class MissedCall {
	private static final String TAG = MainService.TAG + ".MissedCall";

	private Context mContext;

	public MissedCall(Context context) {
		mContext = context;
	}

	public boolean check() {
		Cursor cursor = null;
		try {
			cursor = mContext.getContentResolver().query(Uri.parse("content://call_log/calls"),
					null, null, null, Calls.DATE + " DESC");
			while (cursor.moveToNext()) {
				String callType = cursor.getString(cursor.getColumnIndex(Calls.TYPE));
				String isCallNew = cursor.getString(cursor.getColumnIndex(Calls.NEW));
				if (Integer.parseInt(callType) == Calls.MISSED_TYPE && Integer.parseInt(isCallNew) > 0) {
					cursor.close();
					return true;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Cannot check the call log");
		}
		cursor.close();
		return false;
	}
}
