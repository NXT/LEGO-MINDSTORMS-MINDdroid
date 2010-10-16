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

import backport.android.bluetooth.BluetoothAdapter;
import backport.android.bluetooth.BluetoothDevice;
import backport.android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.UUID;

/**
 * Helper thread class for communication over bluetooth
 */
public class BTCommunicator extends Thread {
	public static final int MOTOR_A = 0;
	public static final int MOTOR_B = 1;
	public static final int MOTOR_C = 2;
	public static final int MOTOR_B_ACTION = 40;
	public static final int MOTOR_RESET = 10;
	public static final int DO_ACTION = 50;
	public static final int DO_BEEP = 51;
	public static final int READ_MOTOR_STATE = 60;
	public static final int DISCONNECT = 99;

	public static final int DISPLAY_TOAST = 1000;
	public static final int STATE_CONNECTED = 1001;
	public static final int STATE_CONNECTERROR = 1002;
	public static final int MOTOR_STATE = 1003;
	public static final int STATE_RECEIVEERROR = 1004;
	public static final int STATE_SENDERROR = 1005;
	
	public static final int NO_DELAY = 0;

	private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// this is the only OUI registered by LEGO, see http://standards.ieee.org/regauth/oui/index.shtml
	private static final String OUI_LEGO = "00:16:53";

	BluetoothAdapter btAdapter;
	private BluetoothSocket nxtBTsocket = null;
	private DataOutputStream nxtDos = null;
	private DataInputStream nxtDin = null;
	private boolean connected = false;

	private Handler uiHandler;
	private String mMACaddress;
	private MINDdroid myMINDdroid;

	private byte[] returnMessage;

	public BTCommunicator(MINDdroid myMINDdroid, Handler uiHandler, BluetoothAdapter btAdapter) {
		this.myMINDdroid = myMINDdroid;
		this.uiHandler = uiHandler;
		this.btAdapter = btAdapter;
	}

	public Handler getHandler() {
		return myHandler;
	}

	public byte[] getReturnMessage() {
		return returnMessage;
	}

	public boolean isBTAdapterEnabled() {
		return (btAdapter == null) ? false : btAdapter.isEnabled();
	}

	@Override
	public void run() {

		createNXTconnection();		

        while (connected) {

	        // read length of message and the message itself
	        int length;
	        try {
		        length = nxtDin.readByte();
		        length = (nxtDin.readByte() << 8) + length;
		        returnMessage = new byte[length];
		        nxtDin.read(returnMessage);
                if ((length >= 2) && (returnMessage[0] == 0x02)) 
                    dispatchMessage(returnMessage);		        
	        } catch (IOException e) {
	            // don't inform the user when connection is already closed
	            if (connected)
	                sendState(STATE_RECEIVEERROR);
		        return;
	        }
        }		
	}

	/**
	 * Create bluetooth-connection with SerialPortServiceClass_UUID
	 * 
	 * @see <a href=
	 *      "http://lejos.sourceforge.net/forum/viewtopic.php?t=1991&highlight=android"
	 *      />
	 */
	private void createNXTconnection() {
		try {

			BluetoothSocket nxtBTsocketTEMPORARY;
			BluetoothDevice nxtDevice = null;
			nxtDevice = btAdapter.getRemoteDevice(mMACaddress);

			if (nxtDevice == null) {
				sendToast(myMINDdroid.getResources().getString(R.string.no_paired_nxt));
				sendState(STATE_CONNECTERROR);
				return;
			}

			nxtBTsocketTEMPORARY = nxtDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
			nxtBTsocketTEMPORARY.connect();
			nxtBTsocket = nxtBTsocketTEMPORARY;

			nxtDin = new DataInputStream(nxtBTsocket.getInputStream());
			nxtDos = new DataOutputStream(nxtBTsocket.getOutputStream());

			connected = true;
		} catch (IOException e) {
			//Log.d("BTCommunicator", "error createNXTConnection()", e);
			if (myMINDdroid.pairing){
    			sendToast(myMINDdroid.getResources().getString(R.string.pairing_message));
	    		sendState(STATE_CONNECTERROR);	
			}
			else {
	    		sendState(STATE_CONNECTERROR);
			}
			return;
		}
		sendState(STATE_CONNECTED);
	}

    private void dispatchMessage(byte[] message) {
        switch (message[1]) {
            // GETOUTPUTSTATE return message
            case 0x06: 
                if (message.length >= 25)
    		        sendState(MOTOR_STATE);
    			break;
        }
    }

	private void destroyNXTconnection() {
			try {
			if (nxtBTsocket != null) {
		        // send stop messages before closing
		        changeMotorSpeed(MOTOR_A, 0);
		        changeMotorSpeed(MOTOR_B, 0);
		        changeMotorSpeed(MOTOR_C, 0);
		        waitSomeTime(500);
                connected = false;
				nxtBTsocket.close();
				nxtBTsocket = null;
			}
			nxtDin = null;
			nxtDos = null;
		} catch (IOException e) {
			sendToast(myMINDdroid.getResources().getString(R.string.problem_at_closing));
		}
	}
	    
	private void doBeep(int frequency, int duration) {
		byte[] message = LCPMessage.getBeepMessage(frequency, duration);
		sendMessage(message);
		waitSomeTime(20);
	}

	private void startProgram(String programName) {
		byte[] message = LCPMessage.getProgramMessage(programName);
		sendMessage(message);
	}

	private void changeMotorSpeed(int motor, int speed) {
		if (speed > 100)
			speed = 100;
		else if (speed < -100)
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

	private void readMotorState(int motor) {
		byte[] message = LCPMessage.getOutputStateMessage(motor);
		sendMessage(message);
	}

	private boolean sendMessage(byte[] message) {
		if (nxtDos == null) {
			return false;
		}

		try {
			// send message length
			int messageLength = message.length;
			nxtDos.writeByte(messageLength);
			nxtDos.writeByte(messageLength >> 8);
			nxtDos.write(message, 0, message.length);
			nxtDos.flush();
			return true;
		} catch (IOException ioe) {
            sendState(STATE_SENDERROR);
			return false;
		}
	}

	private void waitSomeTime(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
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
		@Override
		public void handleMessage(Message myMessage) {

			int message;
			switch (message = myMessage.getData().getInt("message")) {
				case MOTOR_A:
				case MOTOR_B:
				case MOTOR_C:
					changeMotorSpeed(message, myMessage.getData().getInt("value1"));
					break;
				case MOTOR_B_ACTION:
					rotateTo(MOTOR_B, myMessage.getData().getInt("value1"));
					break;
				case MOTOR_RESET:
					reset(myMessage.getData().getInt("value1"));
					break;
				case DO_ACTION:
					startProgram("action.rxe");
					break;
				case DO_BEEP:
					doBeep(myMessage.getData().getInt("value1"), myMessage.getData().getInt("value2"));
					break;
				case READ_MOTOR_STATE:
					readMotorState(myMessage.getData().getInt("value1"));
					break;
				case DISCONNECT:
					destroyNXTconnection();
					break;
			}
		}
	};

	public void setMACAddress(String mMACaddress) {
		this.mMACaddress = mMACaddress;

	}

}
