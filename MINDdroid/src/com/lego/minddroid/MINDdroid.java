/**
 *   Copyright 2010 Guenther Hoelzl, Shawn Brown
 *
 *   This file is part of MINDdroid.
 *
 *   MINDdroid is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   MINDdroid is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with MINDdroid.  If not, see <http://www.gnu.org/licenses/>.
**/

package com.lego.minddroid;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

public class MINDdroid extends Activity {
	public static final int UPDATE_TIME = 200;
	public static final int MENU_INFO = Menu.FIRST;
	public static final int MENU_TOGGLE_CONNECT = Menu.FIRST + 1;
	public static final int MENU_QUIT = Menu.FIRST + 2;
	private static final int REQUEST_CONNECT_DEVICE = 1000;
	private static final int REQUEST_ENABLE_BT = 2000;
	private BTCommunicator myBTCommunicator = null;
	private Toast reusableToast;
	private boolean connected = false;
	private ProgressDialog connectingProgressDialog;
	private Handler btcHandler;
	private Menu myMenu;
	private GameView mView;
    boolean pairing;
	/**
	 * Called when the activity is first created. Inititializes all the
	 * graphical views.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		StartSound mySound = new StartSound(this);
		mySound.start();
		// setup our view, give it focus and display.
		mView = new GameView(getApplicationContext(), this);
		mView.setFocusable(true);
		setContentView(mView);
		reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		// select the nxt to connect to
		selectNXT();
	}

	private void updateButtonsAndMenu() {

		if (myMenu == null)
			return;

		myMenu.removeItem(MENU_TOGGLE_CONNECT);

		if (connected) {
			myMenu.add(0, MENU_TOGGLE_CONNECT, 2, getResources().getString(R.string.disconnect)).setIcon(R.drawable.ic_menu_connected);
		} else {
			myMenu.add(0, MENU_TOGGLE_CONNECT, 2, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
		}
	
	}

	public void createBTCommunicator() {
		// interestingly BT adapter needs to be obtained by the UI thread - so we pass it in in the constructor
		myBTCommunicator = new BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter());
		btcHandler = myBTCommunicator.getHandler();
	}

	public void startBTCommunicator(String mac_address) {
		if (myBTCommunicator == null) {
			createBTCommunicator();
		}

		myBTCommunicator.setMACAddress(mac_address);
		myBTCommunicator.start();
		updateButtonsAndMenu();
		connectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

	}

	public void destroyBTCommunicator() {
		if (myBTCommunicator != null) {
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0);
			myBTCommunicator = null;
		}
		connected = false;
		updateButtonsAndMenu();
	}

	public boolean isConnected() {
		return connected;
	}

	public void actionButtonPressed() {
		if (myBTCommunicator != null) {
			mView.getThread().mActionPressed = true;
			// depending on what the robot should when pressing the action button
			// you have to uncomment/comment one of the following lines

			// sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DO_ACTION, 0, 0);

			// Super Mario Brothers: Main Theme Start
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DO_BEEP, 659, 100);
			sendBTCmessage(150, BTCommunicator.DO_BEEP, 659, 100);
			sendBTCmessage(450, BTCommunicator.DO_BEEP, 659, 250);
			sendBTCmessage(750, BTCommunicator.DO_BEEP, 523, 100);
			sendBTCmessage(900, BTCommunicator.DO_BEEP, 659, 250);
			sendBTCmessage(1200, BTCommunicator.DO_BEEP, 784, 250);
			sendBTCmessage(1800, BTCommunicator.DO_BEEP, 392, 250);

			// MOTOR B: forth an back
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.MOTOR_B, 50, 0);
			sendBTCmessage(600, BTCommunicator.MOTOR_B, -50, 600);
			sendBTCmessage(1200, BTCommunicator.MOTOR_B, 0, 1200);

			sendBTCmessage(1500, BTCommunicator.READ_MOTOR_STATE, BTCommunicator.MOTOR_B, 1500);

		}
	}

	public void updateMotorControl(int left, int right) {

		if (myBTCommunicator != null) {
			// send messages via the handler
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.MOTOR_A, left, 0);
			sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.MOTOR_C, right, 0);
		}
	}

	void sendBTCmessage(int delay, int message, int value1, int value2) {
		Bundle myBundle = new Bundle();
		myBundle.putInt("message", message);
		myBundle.putInt("value1", value1);
		myBundle.putInt("value2", value2);
		Message myMessage = myHandler.obtainMessage();
		myMessage.setData(myBundle);
		if (delay == 0)
			btcHandler.sendMessage(myMessage);
		else
			btcHandler.sendMessageDelayed(myMessage, delay);
	}

	@Override
	public void onResume() {
		super.onResume();
		mView.registerListener();

		if (myBTCommunicator == null) {
			createBTCommunicator();
		}

		// If BT is not on, request that it be enabled.
		if (!myBTCommunicator.isBTAdapterEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyBTCommunicator();
	}

	@Override
	public void onPause() {
		mView.unregisterListener();
		destroyBTCommunicator();
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		mView.unregisterListener();
	}

	/**
	 * Creates the menu items
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		myMenu = menu;
		myMenu.add(0, MENU_INFO, 1, getResources().getString(R.string.about)).setIcon(R.drawable.ic_menu_about);
		myMenu.add(0, MENU_TOGGLE_CONNECT, 2, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
		myMenu.add(0, MENU_QUIT, 3, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
		updateButtonsAndMenu();
		return true;
	}

	/**
	 * Handles item selections
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_INFO:
        		About about = new About();
        		about.show(this);
				return true;
			case MENU_TOGGLE_CONNECT:
				Log.d("MINDdroid", "MENU_CONNECT");
				if (myBTCommunicator == null||connected==false) {
					selectNXT();
				} else {
					Log.d("MINDDroid", "destroyBTCommunicator onOptionsItemSelected");
					destroyBTCommunicator();
					updateButtonsAndMenu();
				}
				return true;
			case MENU_QUIT:
				Log.d("MINDDroid", "destroyBTCommunicator onOptionsItemSelected");
				destroyBTCommunicator();
				finish();
				SplashMenu.quitApplication();
				return true;
		}
		return false;
	}


	private void showToast(String textToShow) {
		reusableToast.setText(textToShow);
		reusableToast.show();
	}

	// receive messages from the BTCommunicator
	final Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message myMessage) {
			switch (myMessage.getData().getInt("message")) {
				case BTCommunicator.DISPLAY_TOAST:
					showToast(myMessage.getData().getString("toastText"));
					break;
				case BTCommunicator.STATE_CONNECTED:
					connected = true;
					connectingProgressDialog.dismiss();
					updateButtonsAndMenu();
					break;
				case BTCommunicator.STATE_CONNECTERROR:
					myBTCommunicator = null;
					connectingProgressDialog.dismiss();
					connected = false;
					updateButtonsAndMenu();
					break;
				case BTCommunicator.MOTOR_STATE:
					if (myBTCommunicator != null) {
						byte[] motorMessage = myBTCommunicator.getReturnMessage();
						int position = byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8) + (byteToInt(motorMessage[23]) << 16)
								+ (byteToInt(motorMessage[24]) << 24);
						showToast(getResources().getString(R.string.current_position) + position);
					}
					break;
			}
		}
	};

	private int byteToInt(byte byteValue) {
		int intValue = (byteValue & (byte) 0x7f);
		if ((byteValue & (byte) 0x80) != 0)
			intValue |= 0x80;
		return intValue;
	}

	void selectNXT() {
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CONNECT_DEVICE:
				// When DeviceListActivity returns with a device to connect
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					pairing=data.getExtras().getBoolean(DeviceListActivity.PAIRING);
					startBTCommunicator(address);
					setContentView(mView);

				}
				break;
			case REQUEST_ENABLE_BT:
				// When the request to enable Bluetooth returns
				if (resultCode != Activity.RESULT_OK) {
					Toast.makeText(this, R.string.problem_at_connecting, Toast.LENGTH_SHORT).show();
					finish();
				}
		}
	}
}
