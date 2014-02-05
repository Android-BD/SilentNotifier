package org.cybione.android.silencer.features;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class UnreadSMS {
	private Context mContext;

	public UnreadSMS(Context context) {
		mContext = context;
	}

	public boolean check() {
		Cursor cursor = mContext.getContentResolver().query(Uri.parse("content://sms/inbox"),
				null, "read = 0", null, null);
		int unreadMessagesCount = cursor.getCount();
		cursor.close();
		if (unreadMessagesCount > 0)
			return true;
		return false;
	}
}
