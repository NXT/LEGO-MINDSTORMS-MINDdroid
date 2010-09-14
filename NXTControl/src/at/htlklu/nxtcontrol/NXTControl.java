package at.htlklu.nxtcontrol;

/*
  NXTControlAndroid application for remote controlling the NXT brick
  2010 by Guenther Hoelzl
  see http://sites.google.com/site/ghoelzl/

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

public class NXTControl extends Activity
{
    public static final int UPDATE_TIME = 200;
    public static final int MENU_ABOUT = Menu.FIRST;
    public static final int MENU_QUIT = Menu.FIRST + 1;

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


    /** 
      * Called when the activity is first created. Inititializes all the graphical views.
      */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        myNXT = (TextView) findViewById(R.id.nxtName);
        myNXT.setText(getResources().getString(R.string.nxt_default_name));
        initSeekBars();
        initConnectButton();
    	initActionButton();
    }


    private void initConnectButton() {        
        connectButton = (Button) findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (myBTCommunicator == null) {
                    createBTCommunicator();
                }
                else {
                    destroyBTCommunicator();
                }
            }
        }); 
    }    


    private void updateButtons() {
        if (connected) {
            connectButton.setText(getResources().getString(R.string.disconnect));
            connectButton.setEnabled(true);
            actionButton.setEnabled(true);
            myNXT.setEnabled(false);
        }
        else {
            connectButton.setText(getResources().getString(R.string.connect));
            connectButton.setEnabled(myBTCommunicator == null);
            actionButton.setEnabled(false);
            myNXT.setEnabled(true);
        }
    }            


    public void createBTCommunicator() {                
        myBTCommunicator = new BTCommunicator(this, myHandler, myNXT.getText().toString());
        if (myBTCommunicator.isBTAdapterEnabled()) {
            btcHandler = myBTCommunicator.getHandler();
            myBTCommunicator.start();
            updateButtons();
            connectingProgressDialog = ProgressDialog.show(this, "", 
                "Connecting to your robot.\nPlease wait...", true);
        }
        else {
            myBTCommunicator = null;
            showToast("Please enable bluetooth first!");
        }
    }


    public void destroyBTCommunicator() {
        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.DISCONNECT, 0);
            myBTCommunicator = null;
        }
        connected = false;
        updateButtons();
    }


    private void initActionButton() {
        actionButton = (Button) findViewById(R.id.action_button);
        actionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                sendBTCmessage(BTCommunicator.ACTION, 440);
            }
        }); 
    }    


    private void initSeekBars() {
        pitchSeekBar = (SeekBar) findViewById(R.id.seekbar1);
        rollSeekBar = (SeekBar) findViewById(R.id.seekbar2);
        pitchSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (runWithEmulator) 
                    updateOrientation((float) 0.0, (float)((progress-50.0)*30.0/50.0), (float)((rollSeekBar.getProgress()-50.0)*30.0/50.0), false);
            }

            public void onStartTrackingTouch(SeekBar s) {
            }

            public void onStopTrackingTouch(SeekBar s) {
            }

        });
        rollSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (runWithEmulator) 
                    updateOrientation((float) 0.0, (float)((pitchSeekBar.getProgress()-50.0)*30.0/50.0), (float)((progress-50.0)*30.0/50.0), false);
            }

            public void onStartTrackingTouch(SeekBar s) {
            }

            public void onStopTrackingTouch(SeekBar s) {
            }

        });
    }


    private final SensorEventListener orientationSensorEventListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            updateOrientation(event.values[0],
                              event.values[1],
                              event.values[2],
                              true);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    private void updateOrientation(float heading, float pitch, float roll, boolean fromSensor) {

        long currentTime;
        int left = 0;
        int right = 0;

        // show position at the seekbars
        if (fromSensor) {
            pitchSeekBar.setProgress((int) (pitch*50.0/30.0 + 50.5));        
            rollSeekBar.setProgress((int) (roll*50.0/30.0 + 50.5));    
        }

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
        List<Sensor> sensorList;

        super.onResume();
        connectButton.setText(getResources().getString(R.string.connect));
        actionButton.setEnabled(false);
                
	    // register orientation sensor
	    sensorList = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        runWithEmulator = (sensorList.size() == 0);
        if (!runWithEmulator)
            sensorManager.registerListener(orientationSensorEventListener, sensorList.get(0), SensorManager.SENSOR_DELAY_GAME);
    }


    @Override
    public void onPause() {
        destroyBTCommunicator();
        sensorManager.unregisterListener(orientationSensorEventListener);
        super.onStop();
    }


    /**
      *  Creates the menu items 
      */
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ABOUT, 0, "About").setIcon(R.drawable.menu_info_icon);
        menu.add(0, MENU_QUIT, 0, "Quit").setIcon(R.drawable.menu_quit_icon);
        return true;
    }


    /**
      *  Handles item selections 
      */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ABOUT:
                showAboutDialog();
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
                    updateButtons();
                    break;
                case BTCommunicator.STATE_CONNECTERROR:
                    myBTCommunicator = null;
                    connectingProgressDialog.dismiss();
                    updateButtons();
                    break;
            }
        }
    };

}
