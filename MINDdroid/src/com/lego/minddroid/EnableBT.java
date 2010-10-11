package com.lego.minddroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class EnableBT extends Activity { //currently unused.  Will be implemented to allow connection without user having to say "Yes turn bt on" (when it isn't)

	boolean processStarted = false;
	StatusReciever statusReciever;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(Activity.RESULT_CANCELED);

		statusReciever = new StatusReciever();
		registerReceiver(statusReciever, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
		processStarted = turnOnBt();
		if (!processStarted) {
			sendFailureStatus();
		}
	}

	private boolean turnOnBt() {

		return BluetoothAdapter.getDefaultAdapter().enable();
	}

	public void sendFailureStatus() {

		Log.d("EnableBT sendFailureStatus", "RESULT_CANCELED");
		this.setResult(Activity.RESULT_CANCELED);
		finish();
	}

	public void sendSuccessStatus() {
		Log.d("EnableBT sendSuccessStatus", "RESULT_OK");
		this.setResult(Activity.RESULT_OK);
		finish();
	}

	@Override
	protected void onPause() {
		super.onPause();
		 try {
			unregisterReceiver(statusReciever);
		} catch (Exception e) {
			// not registered
			 
		}
	}
	
	@Override
	protected void onDestroy() {
		try {
			unregisterReceiver(statusReciever);
		} catch (Exception e) {
			// not registered
		}
		super.onDestroy();
 
	}

	public class StatusReciever extends BroadcastReceiver {

		/**
		 * 
		 */

		public final static String STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED";

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("EnableBT statusReciever", "onReceive");
			if (intent.getAction().equals(STATE_CHANGED)) {
				Log.d("EnableBT statusReciever", "ACTION_STATE_CHANGED");
				sendSuccessStatus();
			} else {
				Log.d("EnableBT statusReciever", "fail: " + intent.getAction());
				sendFailureStatus();

			}

		}
	}

}
