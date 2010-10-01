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
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
        StartSound mySound = new StartSound(this);
        mySound.start();
		// setup our view, give it focus and display.
		mView = new GameView(getApplicationContext(), this);
		mView.setFocusable(true);
		setContentView(mView);
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT); 
        // now start the bluetooth connection automatically
        createBTCommunicator();       
    }


    private void updateButtonsAndMenu() {

        if (myMenu == null)
            return;
    
        myMenu.removeItem(MENU_CONNECT);

        if (connected) {
            myMenu.add(0, MENU_CONNECT, 2, getResources().getString(R.string.disconnect)).setIcon(R.drawable.ic_menu_connected);
        }
        else {
            myMenu.add(0, MENU_CONNECT, 2, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        }
    }            


    public void createBTCommunicator() {                
        myBTCommunicator = new BTCommunicator(this, myHandler, getResources().getString(R.string.nxt_default_name));
        if (myBTCommunicator.isBTAdapterEnabled()) {
            btcHandler = myBTCommunicator.getHandler();
            myBTCommunicator.start();
            updateButtonsAndMenu();
            connectingProgressDialog = ProgressDialog.show(this, "", 
                getResources().getString(R.string.connecting_please_wait), true);
        }
        else {
            myBTCommunicator = null;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);
        }
    }


    public void destroyBTCommunicator() {
        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.DISCONNECT, 0, 0);
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
            mView.getThread().mActionPressed=true;
            // depending on what the robot should when pressing the action button
            // you have to uncomment/comment one of the following lines

            // sendBTCmessage(BTCommunicator.DO_ACTION, 0);

            sendBTCmessage(BTCommunicator.DO_BEEP, 440, 0);

            sendBTCmessage(BTCommunicator.MOTOR_B, 50, 0);
            sendBTCmessage(BTCommunicator.MOTOR_B, -50, 600);
            sendBTCmessage(BTCommunicator.MOTOR_B, 0, 1200);            

            sendBTCmessage(BTCommunicator.READ_MOTOR_STATE, BTCommunicator.MOTOR_B, 1500);
            
        }
    }

    
    public void updateMotorControl(float pitch, float roll) {
        
        int left = 0;
        int right = 0;
        if (myBTCommunicator != null) {

            // only when phone is little bit tilted
            if ((Math.abs(pitch) > 10.0) || (Math.abs(roll) > 10.0)) {
            
                // limit pitch and roll
                if (pitch > 33.3)
                    pitch = (float) 33.3;
                else    
                if (pitch < -33.3)
                    pitch = (float) -33.3;

                if (roll > 33.3)
                    roll = (float) 33.3;
                else    
                if (roll < -33.3)
                    roll = (float) -33.3;
                
                // when pitch is very small then do a special turning function    
                if (Math.abs(pitch) > 10.0) {
                    left = (int) Math.round(3.3*pitch * (1.0 + roll / 60.0));
                    right = (int) Math.round(3.3*pitch * (1.0 - roll / 60.0));             
                }
                else {
                    left = (int) Math.round(3.3*roll - Math.signum(roll)*3.3*Math.abs(pitch));
                    right = -left;
                }    

                // limit the motor outputs

                if (left > 100)
                    left = 100;
                else    
                if (left < -100)
                    left = -100;
                    
                if (right > 100)
                    right = 100;
                else    
                if (right < -100)
                    right = -100;

            }              

            // send messages via the handler
            sendBTCmessage(BTCommunicator.MOTOR_A, left, 0);
            sendBTCmessage(BTCommunicator.MOTOR_C, right, 0);
        }
    }
    

    void sendBTCmessage(int message, int value, int delay) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putInt("value", value);
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
        myMenu.add(0, MENU_INFO, 1, getResources().getString(R.string.info)).setIcon(R.drawable.ic_menu_about);
        myMenu.add(0, MENU_CONNECT, 2, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        myMenu.add(0, MENU_QUIT, 3, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
        updateButtonsAndMenu();
        return true;
    }


    /**
      *  Handles item selections 
      */
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_INFO:
				Info.show(this);
                // showAboutDialog();
                return true;       
            case MENU_CONNECT:
                if (myBTCommunicator == null) {
                    createBTCommunicator();
                }
                else {
                    destroyBTCommunicator();
                    updateButtonsAndMenu();
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
                    updateButtonsAndMenu();
                    break;
                case BTCommunicator.MOTOR_STATE: 
                    if (myBTCommunicator != null) {
                        byte[] motorMessage = myBTCommunicator.getReturnMessage();
                        int position = byteToInt(motorMessage[21]) + 
                            (byteToInt(motorMessage[22]) << 8) + 
                            (byteToInt(motorMessage[23]) << 16) + 
                            (byteToInt(motorMessage[24]) << 24);
                            showToast(getResources().getString(R.string.current_position) + position);
                    }
                    break;                           
            }
        }
    };


    private int byteToInt(byte byteValue) {
        int intValue = (int) (byteValue & (byte) 0x7f);
        if ((byteValue & (byte) 0x80) != 0)
            intValue |= 0x80;
        return intValue;
    }        
    

}
