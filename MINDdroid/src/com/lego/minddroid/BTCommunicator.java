package com.lego.minddroid;

/*
  BTCommunicator is a helper thread for communication to
  the NXT brick over bluetooth

  This file is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
*/

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.*;
import java.util.Set;
import java.util.UUID;

/**
 * Helper thread class for communication over bluetooth
 */
public class BTCommunicator extends Thread
{
    public static final int MOTOR_A = 0;
    public static final int MOTOR_B = 1;
    public static final int MOTOR_C = 2;
    public static final int MOTOR_B_ACTION = 40;
    public static final int MOTOR_RESET = 10;
    public static final int ACTION=50;
    public static final int DISCONNECT = 99;  

    public static final int DISPLAY_TOAST = 1000;
    public static final int STATE_CONNECTED = 1001;
    public static final int STATE_CONNECTERROR = 1002;

    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter btAdapter;
    private BluetoothSocket nxtBTsocket = null;
    private DataOutputStream nxtDos = null;
    private DataInputStream nxtDin = null;

    private Handler uiHandler;
    private String myNXTName;
    private MINDdroid myMINDdroid;

    public BTCommunicator(MINDdroid myMINDdroid, Handler uiHandler, String myNXTName) {
        this.myMINDdroid = myMINDdroid;
        this.myNXTName = myNXTName;
        this.uiHandler = uiHandler;
    }

    public Handler getHandler() {
        return myHandler;
    }

    public boolean isBTAdapterEnabled() {
        // interestingly this has to be done by the UI thread
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        return (btAdapter == null) ? false : btAdapter.isEnabled();
    }

    @Override
    public void run() {
        createNXTConnection();
        // wait for Bluetooth-Messages and send it to the UI-thread
        // not implemented yet
    }

    /**
      * Create bluetooth-connection with SerialPortServiceClass_UUID
      * @see <a href="http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android" />
      */
    private void createNXTConnection() {
        try {
            Set<BluetoothDevice> bondedDevices = btAdapter.getBondedDevices();
            BluetoothDevice nxtDevice = null;
         
            for (BluetoothDevice bluetoothDevice : bondedDevices)
            {
                if (bluetoothDevice.getName().equals(myNXTName.toString())) {
                    nxtDevice = bluetoothDevice;
                    break;
                }
            } 

            if (nxtDevice == null)
            {
                sendToast("No paired NXT device found");
                sendState(STATE_CONNECTERROR);
                return;
            }             

            nxtBTsocket = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
            nxtBTsocket.connect();
            nxtDin = new DataInputStream(nxtBTsocket.getInputStream());
            nxtDos = new DataOutputStream(nxtBTsocket.getOutputStream());          
        } catch (IOException e) {
            sendToast("Problem at creating a connection");
            sendState(STATE_CONNECTERROR);
            return;
        }
        sendState(STATE_CONNECTED);
    }
        

    private void destroyNXTConnection() {
        try {
            if (nxtBTsocket != null) {
                // send stop messages before closing
                changeMotorSpeed(MOTOR_A, 0);
                changeMotorSpeed(MOTOR_C, 0);
                waitSomeTime(500);
                nxtBTsocket.close();
                nxtBTsocket = null;
            }
            nxtDin = null;
            nxtDos = null;            
        } catch (IOException e) {
            sendToast("Problem at closing the connection");
        }
    }


    private void doBeep(int frequency, int duration) {
        byte[] message = LCPMessage.getBeepMessage(frequency, duration);
        sendMessage(message);
        waitSomeTime(20);
    }


    private void changeMotorSpeed(int motor, int speed) {
        if (speed > 100) 
            speed = 100;
        else
        if (speed < -100)
            speed = -100;

        byte[] message = LCPMessage.getMotorMessage(motor, speed);
        sendMessage(message);
    }


    private void rotateTo(int motor, int end) {
        byte[] message = LCPMessage.getMotorMessage(motor, -80, end);
        sendMessage(message);
    }


    private void reset(int motor) {
        byte[] message = LCPMessage.getResetMessage(motor);
        sendMessage(message);
    }


    private void sendMessage(byte[] message) {
        if (nxtDos == null) {
            return;
        }

        try {
            nxtDos.write(message, 0, message.length);
            nxtDos.flush();        
        } catch (IOException ioe) { 
            sendToast("Problem at sending message!");
        }
    }        
        

    private void waitSomeTime(int millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
        }
    }


    private void sendToast(String toastText) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", DISPLAY_TOAST);
        myBundle.putString("toastText", toastText);
        sendBundle(myBundle);
    }


    private void sendState(int message) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        sendBundle(myBundle);
    }

       
    private void sendBundle(Bundle myBundle) {
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);
        uiHandler.sendMessage(myMessage);
    }


    // receive messages from the UI
    final Handler myHandler = new Handler() {
        public void handleMessage(Message myMessage) {
            int message;
            switch (message = myMessage.getData().getInt("message")) {
                case MOTOR_A: 
                case MOTOR_B:
                case MOTOR_C: 
                    changeMotorSpeed(message, myMessage.getData().getInt("value"));
                    break;
                case MOTOR_B_ACTION:
                    rotateTo(MOTOR_B, myMessage.getData().getInt("value"));
                    break;
                case MOTOR_RESET:
                    reset(myMessage.getData().getInt("value"));
                    break;
                case ACTION:
                    doBeep(myMessage.getData().getInt("value"), 1000);
                    break;
                case DISCONNECT:
                    destroyNXTConnection();
                    break;
            }
        }
    };
       
}

