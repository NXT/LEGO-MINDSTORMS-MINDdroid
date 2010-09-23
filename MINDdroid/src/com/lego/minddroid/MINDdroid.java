package com.lego.minddroid;

/*
  MINDdroid application for remote controlling the NXT brick

  This file is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
*/

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Enumeration;

public class MINDdroid extends Activity
{
    public static final int UPDATE_TIME = 200;
    public static final int MENU_INFO = Menu.FIRST;
    public static final int MENU_CONNECT = Menu.FIRST + 1;
    public static final int MENU_QUIT = Menu.FIRST + 2;

    private SensorManager sensorManager;
    private boolean runWithEmulator = false;
    private SeekBar pitchSeekBar;
    private SeekBar rollSeekBar;
    private Button connectButton;
    private Button actionButton;
    private TextView myNXT;
    private long timeDataSent = 0;
    private BTCommunicator myBTCommunicator = null;
    private Toast reusableToast;
    private boolean connected = false;
    private ProgressDialog connectingProgressDialog;
    private Handler btcHandler;
    private Menu myMenu;

	private GameView mView;

    /** 
      * Called when the activity is first created. Inititializes all the graphical views.
      */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		// setup our view, give it focus and display.
		mView = new GameView(getApplicationContext(), this);
		mView.setFocusable(true);
		setContentView(mView);
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        // at the moment disabled because of interference with the accelerometer
        // StartSound mySound = new StartSound(this);
        // mySound.start();
    }


    private void updateButtonsAndMenu() {
        myMenu.removeItem(MENU_CONNECT);

        if (connected) {
            myMenu.add(0, MENU_CONNECT, 2, "Disconnect").setIcon(R.drawable.ic_menu_connected);
        }
        else {
            myMenu.add(0, MENU_CONNECT, 2, "Connect").setIcon(R.drawable.ic_menu_connect);
        }
    }            


    public void createBTCommunicator() {                
        myBTCommunicator = new BTCommunicator(this, myHandler, getResources().getString(R.string.nxt_default_name));
        if (myBTCommunicator.isBTAdapterEnabled()) {
            btcHandler = myBTCommunicator.getHandler();
            myBTCommunicator.start();
            updateButtonsAndMenu();
            connectingProgressDialog = ProgressDialog.show(this, "", 
                "Connecting to your robot.\nPlease wait...", true);
        }
        else {
            myBTCommunicator = null;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }


    public void destroyBTCommunicator() {
        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.DISCONNECT, 0);
            myBTCommunicator = null;
        }
        connected = false;
       // updateButtonsAndMenu();
    }


    public void actionButtonPressed() {
        if (myBTCommunicator != null)            
            sendBTCmessage(BTCommunicator.ACTION, 440);
            mView.getThread().mActionPressed=true;
            // will have to implement a seperate thread for waiting a special time
            // the below implemented commands don't work correctly
            // sendBTCmessage(BTCommunicator.MOTOR_RESET, BTCommunicator.MOTOR_B);
            // sendBTCmessage(BTCommunicator.MOTOR_B_ACTION, 200);
    }


    public void updateOrientation(float heading, float pitch, float roll, boolean fromSensor) {

        long currentTime;
        int left = 0;
        int right = 0;

        // send values to NXT periodically
        if (myBTCommunicator != null) {
            currentTime = System.currentTimeMillis();
            if ((currentTime - timeDataSent) > UPDATE_TIME) {
                timeDataSent = currentTime;

                // calculate motor values only for larger pitch values
                if (Math.abs(pitch) >= 10) {
                    left = (int) Math.round(3.3*pitch * (1.0 + roll / 90.0));
                    right = (int) Math.round(3.3*pitch * (1.0 - roll / 90.0));                
                }              

                // send messages via the handler
                sendBTCmessage(BTCommunicator.MOTOR_A, left);
                sendBTCmessage(BTCommunicator.MOTOR_C, right);
            }
        }
    }


    private void sendBTCmessage(int message, int value) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value", value);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);
        btcHandler.sendMessage(myMessage);        
    }


    @Override
    public void onResume() {
		super.onResume();
		mView.registerListener();
	
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
      *  Creates the menu items 
      */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;
        myMenu.add(0, MENU_INFO, 1, "Info").setIcon(R.drawable.ic_menu_about);
        myMenu.add(0, MENU_CONNECT, 2, "Connect").setIcon(R.drawable.ic_menu_connect);
        myMenu.add(0, MENU_QUIT, 3, "Quit").setIcon(R.drawable.ic_menu_exit);
        return true;
    }


    /**
      *  Handles item selections 
      */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_INFO:
                showAboutDialog();
                return true;       
            case MENU_CONNECT:
                if (myBTCommunicator == null) {
                    createBTCommunicator();
                }
                else {
                    destroyBTCommunicator();
                }
                return true;  
            case MENU_QUIT:
                destroyBTCommunicator();
                finish(); 
                return true;
        }
        return false;
    }


    private void showAboutDialog() 
    {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.aboutbox);
        dialog.show();
    }


    private void showToast(String textToShow) {
        reusableToast.setText(textToShow);
        reusableToast.show();
    }


    // receive messages from the BTCommunicator
    final Handler myHandler = new Handler() {
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
                    updateButtonsAndMenu();
                    break;
            }
        }
    };

}
