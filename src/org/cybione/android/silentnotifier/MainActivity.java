package org.cybione.android.silentnotifier;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Toast toast;
		String name = (String) getResources().getText(R.string.app_name);
		if (!BackgroundService.isStarted()) {
			toast = Toast.makeText(this, "Starting " + name, Toast.LENGTH_SHORT);
			this.startService(new Intent(this, BackgroundService.class));
		} else 
			toast = Toast.makeText(this, name + " is running", Toast.LENGTH_SHORT);
		toast.show();
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

}
