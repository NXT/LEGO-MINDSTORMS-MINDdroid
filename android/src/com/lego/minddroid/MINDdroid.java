/**
 * Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
 *
 * This file is part of MINDdroid.
 *
 * MINDdroid is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MINDdroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MINDdroid. If not, see <http://www.gnu.org/licenses/>.
 **/

package com.lego.minddroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

/**
 * This class is for talking to a LEGO NXT robot and controlling it
 * via bluetooth and the built in acceleration sensor.
 * The communciation to the robot is done via LCP (LEGO communication protocol), 
 * so no special software has to be installed on the robot.
 */
public class MINDdroid extends Activity implements BTConnectable, TextToSpeech.OnInitListener {

    public static final int UPDATE_TIME = 200;
    public static final int MENU_TOGGLE_CONNECT = Menu.FIRST;
    public static final int MENU_START_SW = Menu.FIRST + 1;
    public static final int MENU_QUIT = Menu.FIRST + 2;
    
    public static final int ACTION_BUTTON_SHORT = 0;
    public static final int ACTION_BUTTON_LONG = 1;
    
    private static final int REQUEST_CONNECT_DEVICE = 1000;
    private static final int REQUEST_ENABLE_BT = 2000;
    private BTCommunicator myBTCommunicator = null;
    private boolean connected = false;
    private ProgressDialog connectingProgressDialog;
    private Handler btcHandler;
    private Menu myMenu;
    private GameView mView;
    private Activity thisActivity;
    private boolean btErrorPending = false;
    private boolean pairing;
    private static boolean btOnByUs = false;
    int mRobotType;
    int motorLeft;
    private int directionLeft; // +/- 1
    int motorRight;
    private boolean stopAlreadySent = false;
    private int directionRight; // +/- 1
    private int motorAction;
    private int directionAction; // +/- 1
    private List<String> programList;
    private static final int MAX_PROGRAMS = 20;
    private String programToStart;
    private Toast reusableToast;
    
    // experimental TTS support
    private TextToSpeech mTts;
    private final int TTS_CHECK_CODE = 9991;

    /**
     * Asks if bluetooth was switched on during the runtime of the app. For saving 
     * battery we switch it off when the app is terminated.
     * @return true, when bluetooth was switched on by the app
     */
    public static boolean isBtOnByUs() {
        return btOnByUs;
    }

    /**
     * Sets a flag when bluetooth was switched on durin runtime
     * @param btOnByUs true, when bluetooth was switched on by the app
     */
    public static void setBtOnByUs(boolean btOnByUs) {
        MINDdroid.btOnByUs = btOnByUs;
    }

    /**
     * @return true, when currently pairing 
     */
    @Override
    public boolean isPairing() {
        return pairing;
    }

    /**
     * Called when the activity is first created. Inititializes all the
     * graphical views.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;
        mRobotType = this.getIntent().getIntExtra(SplashMenu.MINDDROID_ROBOT_TYPE, 
            R.id.robot_type_shooterbot);
        setUpByType();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        StartSound mySound = new StartSound(this);
        mySound.start();
        // setup our view, give it focus and display.
        mView = new GameView(getApplicationContext(), this);
        mView.setFocusable(true);
        setContentView(mView);
        reusableToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        // experimental TTS support for the lejosMINDdroid project
        mTts = new TextToSpeech(this,
            this  // TextToSpeech.OnInitListener
            );
    }

    /**
     * Initialization of the motor commands for the different robot types.
     */
    private void setUpByType() {
        switch (mRobotType) {
            case R.id.robot_type_tribot:
                motorLeft = BTCommunicator.MOTOR_B;
                directionLeft = 1;
                motorRight = BTCommunicator.MOTOR_C;
                directionRight = 1;
                motorAction = BTCommunicator.MOTOR_A;
                directionAction = 1;
                break;
            case R.id.robot_type_robogator:
                motorLeft = BTCommunicator.MOTOR_C;
                directionLeft = -1;
                motorRight = BTCommunicator.MOTOR_B;
                directionRight = -1;
                motorAction = BTCommunicator.MOTOR_A;
                directionAction = 1;
                break;
            default:
                // default
                motorLeft = BTCommunicator.MOTOR_B;
                directionLeft = 1;
                motorRight = BTCommunicator.MOTOR_C;
                directionRight = 1;
                motorAction = BTCommunicator.MOTOR_A;
                directionAction = 1;
                break;
        }
    }

    /**
     * Updates the menus and possible buttons when connection status changed.
     */
    private void updateButtonsAndMenu() {

        if (myMenu == null)
            return;

        myMenu.removeItem(MENU_TOGGLE_CONNECT);

        if (connected) {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.disconnect)).setIcon(R.drawable.ic_menu_connected);

        } else {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        }

    }

    /**
     * Creates a new object for communication to the NXT robot via bluetooth and fetches the corresponding handler.
     */
    private void createBTCommunicator() {
        // interestingly BT adapter needs to be obtained by the UI thread - so we pass it in in the constructor
        myBTCommunicator = new BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter(), getResources());
        btcHandler = myBTCommunicator.getHandler();
    }

    /**
     * Creates and starts the a thread for communication via bluetooth to the NXT robot.
     * @param mac_address The MAC address of the NXT robot.
     */
    private void startBTCommunicator(String mac_address) {
        connected = false;        
        connectingProgressDialog = ProgressDialog.show(this, "", getResources().getString(R.string.connecting_please_wait), true);

        if (myBTCommunicator != null) {
            try {
                myBTCommunicator.destroyNXTconnection();
            }
            catch (IOException e) { }
        }
        createBTCommunicator();
        myBTCommunicator.setMACAddress(mac_address);
        myBTCommunicator.start();
        updateButtonsAndMenu();
    }

    /**
     * Sends a message for disconnecting to the communcation thread.
     */
    public void destroyBTCommunicator() {

        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0);
            myBTCommunicator = null;
        }

        connected = false;
        updateButtonsAndMenu();
    }

    /**
     * Gets the current connection status.
     * @return the current connection status to the robot.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Does something special depending on the robot-type.
     * @param buttonMode short, long or other press types.
     */
    private void performActionCommand(int buttonMode) {
        
        if (mRobotType != R.id.robot_type_lejos) {
            if (buttonMode == ACTION_BUTTON_SHORT) {
                // Wolfgang Amadeus Mozart 
                // "Zauberfloete - Der Vogelfaenger bin ich ja"
                sendBTCmessage(BTCommunicator.NO_DELAY, 
                    BTCommunicator.DO_BEEP, 392, 100);
                sendBTCmessage(200, BTCommunicator.DO_BEEP, 440, 100);
                sendBTCmessage(400, BTCommunicator.DO_BEEP, 494, 100);
                sendBTCmessage(600, BTCommunicator.DO_BEEP, 523, 100);
                sendBTCmessage(800, BTCommunicator.DO_BEEP, 587, 300);
                sendBTCmessage(1200, BTCommunicator.DO_BEEP, 523, 300);
                sendBTCmessage(1600, BTCommunicator.DO_BEEP, 494, 300);
            }
            else {
                // G-F-E-D-C
                sendBTCmessage(BTCommunicator.NO_DELAY, 
                    BTCommunicator.DO_BEEP, 392, 100);
                sendBTCmessage(200, BTCommunicator.DO_BEEP, 349, 100);
                sendBTCmessage(400, BTCommunicator.DO_BEEP, 330, 100);
                sendBTCmessage(600, BTCommunicator.DO_BEEP, 294, 100);
                sendBTCmessage(800, BTCommunicator.DO_BEEP, 262, 300);
            }
        }

        // MOTOR ACTION: forth an back
        switch (mRobotType) {
            
            case R.id.robot_type_robogator:
                // Robogator: bite the user in any case ;-)
                for (int bite=0; bite<3; bite++) {
                    sendBTCmessage(bite*400, motorAction, 
                        75*directionAction, 0);
                    sendBTCmessage(bite*400+200, motorAction, 
                        -75*directionAction, 0);
                }    
                sendBTCmessage(3*400, motorAction, 0, 0);
                break;
                
            case R.id.robot_type_lejos:
                // lejosMINDdroid: just send the message for button press
                sendBTCmessage(BTCommunicator.NO_DELAY, 
                    BTCommunicator.DO_ACTION, buttonMode, 0);
                break;                    
        
            default:
                // other robots: 180 degrees forth and back
                int direction = (buttonMode == ACTION_BUTTON_SHORT ? 1 : -1);                
                sendBTCmessage(BTCommunicator.NO_DELAY, motorAction, 
                    75*direction*directionAction, 0);
                sendBTCmessage(500, motorAction, 
                    -75*direction*directionAction, 0);
                sendBTCmessage(1000, motorAction, 0, 0);
                break;
        }
    }

    /**
     * Method for performing the appropriate action when the ACTION button is pressed shortly.
     */
    public void actionButtonPressed() {
        if (myBTCommunicator != null) {
            mView.getThread().mActionPressed = true;
            performActionCommand(ACTION_BUTTON_SHORT);            
        }
    }

    /**
     * Method for performing the appropriate action when the ACTION button is long pressed.
     */
    public void actionButtonLongPress() {
        if (myBTCommunicator != null) {
            mView.getThread().mActionPressed = true;
            performActionCommand(ACTION_BUTTON_LONG);
        }
    }

    /**
     * Starts a program on the NXT robot.
     * @param name The program name to start. Has to end with .rxe on the LEGO firmware and with .nxj on the 
     *             leJOS NXJ firmware.
     */   
    public void startProgram(String name) {
        // for .rxe programs: get program name, eventually stop this and start the new one delayed
        // is handled in startRXEprogram()
        if (name.endsWith(".rxe")) {
            programToStart = name;        
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_PROGRAM_NAME, 0, 0);
            return;
        }
              
        // for .nxj programs: stop bluetooth communication after starting the program
        if (name.endsWith(".nxj")) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, name);
            destroyBTCommunicator();
            return;
        }        

        // for all other programs: just start the program
        sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, name);
    }

    /**
     * Depending on the status (whether the program runs already) we stop it, wait and restart it again.
     * @param status The current status, 0x00 means that the program is already running.
     */   
    public void startRXEprogram(byte status) {
        if (status == 0x00) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.STOP_PROGRAM, 0, 0);
            sendBTCmessage(1000, BTCommunicator.START_PROGRAM, programToStart);
        }    
        else {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, programToStart);
        }
    }        

    /**
     * Sends the motor control values to the communcation thread.
     * @param left The power of the left motor from 0 to 100.
     * @param rigth The power of the right motor from 0 to 100.
     */   
    public void updateMotorControl(int left, int right) {

        if (myBTCommunicator != null) {
            // don't send motor stop twice
            if ((left == 0) && (right == 0)) {
                if (stopAlreadySent)
                    return;
                else
                    stopAlreadySent = true;
            }
            else
                stopAlreadySent = false;         
                        
            // send messages via the handler
            sendBTCmessage(BTCommunicator.NO_DELAY, motorLeft, left * directionLeft, 0);
            sendBTCmessage(BTCommunicator.NO_DELAY, motorRight, right * directionRight, 0);
        }
    }

    /**
     * Sends the message via the BTCommuncator to the robot.
     * @param delay time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param value1 first parameter
     * @param value2 second parameter
     */   
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

    /**
     * Sends the message via the BTCommuncator to the robot.
     * @param delay time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param String a String parameter
     */       
    void sendBTCmessage(int delay, int message, String name) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        myBundle.putString("name", name);
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
        try {
            mView.registerListener();
        }
        catch (IndexOutOfBoundsException ex) {
            showToast(R.string.sensor_initialization_failure, Toast.LENGTH_LONG);
            destroyBTCommunicator();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        // no bluetooth available
        if (BluetoothAdapter.getDefaultAdapter()==null) {
            showToast(R.string.bt_initialization_failure, Toast.LENGTH_LONG);
            destroyBTCommunicator();
            finish();
            return;
        }            

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            selectNXT();
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
        myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect);
        myMenu.add(0, MENU_START_SW, 2, getResources().getString(R.string.start)).setIcon(R.drawable.ic_menu_start);
        myMenu.add(0, MENU_QUIT, 3, getResources().getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit);
        updateButtonsAndMenu();
        return true;
    }
    
    /**
     * Enables/disables the menu items
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean displayMenu;
        displayMenu = super.onPrepareOptionsMenu(menu);
        if (displayMenu) {
            boolean startEnabled = false;
            if (myBTCommunicator != null) 
                startEnabled = myBTCommunicator.isConnected();
            menu.findItem(MENU_START_SW).setEnabled(startEnabled);
        }
        return displayMenu;
    }
    
    /**
     * Handles item selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_TOGGLE_CONNECT:

                if (myBTCommunicator == null || connected == false) {
                    selectNXT();

                } else {
                    destroyBTCommunicator();
                    updateButtonsAndMenu();
                }

                return true;
                
            case MENU_START_SW:
                if (programList.size() == 0) {
                    showToast(R.string.no_programs_found, Toast.LENGTH_SHORT);
                    break;
                }
                
                FileDialog myFileDialog = new FileDialog(this, programList);    		    	    		
                myFileDialog.show(mRobotType == R.id.robot_type_lejos);
                return true;
                
            case MENU_QUIT:
                destroyBTCommunicator();
                finish();

                if (btOnByUs)
                    showToast(R.string.bt_off_message, Toast.LENGTH_SHORT);

                SplashMenu.quitApplication();
                return true;
        }

        return false;
    }

    /**
     * Displays a message as a toast
     * @param textToShow the message
     * @param length the length of the toast to display
     */
    private void showToast(String textToShow, int length) {
        reusableToast.setText(textToShow);
        reusableToast.setDuration(length);
        reusableToast.show();
    }

    /**
     * Displays a message as a toast
     * @param resID the ressource ID to display
     * @param length the length of the toast to display
     */
    private void showToast(int resID, int length) {
        reusableToast.setText(resID);
        reusableToast.setDuration(length);
        reusableToast.show();
    }
    
    /**
     * Receive messages from the BTCommunicator
     */
    final Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message myMessage) {
            switch (myMessage.getData().getInt("message")) {
                case BTCommunicator.DISPLAY_TOAST:
                    showToast(myMessage.getData().getString("toastText"), Toast.LENGTH_SHORT);
                    break;
                case BTCommunicator.STATE_CONNECTED:
                    connected = true;
                    programList = new ArrayList<String>();
                    connectingProgressDialog.dismiss();
                    updateButtonsAndMenu();
                    sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_FIRMWARE_VERSION, 0, 0);
                    break;
                case BTCommunicator.MOTOR_STATE:

                    if (myBTCommunicator != null) {
                        byte[] motorMessage = myBTCommunicator.getReturnMessage();
                        int position = byteToInt(motorMessage[21]) + (byteToInt(motorMessage[22]) << 8) + (byteToInt(motorMessage[23]) << 16)
                                       + (byteToInt(motorMessage[24]) << 24);
                        showToast(getResources().getString(R.string.current_position) + position, Toast.LENGTH_SHORT);
                    }

                    break;

                case BTCommunicator.STATE_CONNECTERROR_PAIRING:
                    connectingProgressDialog.dismiss();
                    destroyBTCommunicator();
                    break;

                case BTCommunicator.STATE_CONNECTERROR:
                    connectingProgressDialog.dismiss();
                case BTCommunicator.STATE_RECEIVEERROR:
                case BTCommunicator.STATE_SENDERROR:

                    destroyBTCommunicator();
                    if (btErrorPending == false) {
                        btErrorPending = true;
                        // inform the user of the error with an AlertDialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
                        builder.setTitle(getResources().getString(R.string.bt_error_dialog_title))
                        .setMessage(getResources().getString(R.string.bt_error_dialog_message)).setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                btErrorPending = false;
                                dialog.cancel();
                                selectNXT();
                            }
                        });
                        builder.create().show();
                    }

                    break;

                case BTCommunicator.FIRMWARE_VERSION:

                    if (myBTCommunicator != null) {
                        byte[] firmwareMessage = myBTCommunicator.getReturnMessage();
                        // check if we know the firmware
                        boolean isLejosMindDroid = true;
                        for (int pos=0; pos<4; pos++) {
                            if (firmwareMessage[pos + 3] != LCPMessage.FIRMWARE_VERSION_LEJOSMINDDROID[pos]) {
                                isLejosMindDroid = false;
                                break;
                            }
                        }
                        if (isLejosMindDroid) {
                            mRobotType = R.id.robot_type_lejos;
                            setUpByType();
                        }
                        // afterwards we search for all files on the robot
                        sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES, 0, 0);
                    }

                    break;

                case BTCommunicator.FIND_FILES:

                    if (myBTCommunicator != null) {
                        byte[] fileMessage = myBTCommunicator.getReturnMessage();
                        String fileName = new String(fileMessage, 4, 20);
                        fileName = fileName.replaceAll("\0","");

                        if (mRobotType == R.id.robot_type_lejos || fileName.endsWith(".nxj") || fileName.endsWith(".rxe")) {
                            programList.add(fileName);
                        }

                        // find next entry with appropriate handle, 
                        // limit number of programs (in case of error (endless loop))
                        if (programList.size() <= MAX_PROGRAMS)
                            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES,
                                           1, byteToInt(fileMessage[3]));
                    }

                    break;
                    
                case BTCommunicator.PROGRAM_NAME:
                    if (myBTCommunicator != null) {
                        byte[] returnMessage = myBTCommunicator.getReturnMessage();
                        startRXEprogram(returnMessage[2]);
                    }
                    
                    break;
                    
                case BTCommunicator.SAY_TEXT:
                    if (myBTCommunicator != null) {
                        byte[] textMessage = myBTCommunicator.getReturnMessage();
                        // evaluate control byte 
                        byte controlByte = textMessage[2];
                        // BIT7: Language
                        if ((controlByte & 0x80) == 0x00) 
                            mTts.setLanguage(Locale.US);
                        else
                            mTts.setLanguage(Locale.getDefault());
                        // BIT6: Pitch
                        if ((controlByte & 0x40) == 0x00)
                            mTts.setPitch(1.0f);
                        else
                            mTts.setPitch(0.75f);
                        // BIT0-3: Speech Rate    
                        switch (controlByte & 0x0f) {
                            case 0x01: 
                                mTts.setSpeechRate(1.5f);
                                break;                                 
                            case 0x02: 
                                mTts.setSpeechRate(0.75f);
                                break;
                            
                            default: mTts.setSpeechRate(1.0f);
                                break;
                        }
                                                                                                        
                        String ttsText = new String(textMessage, 3, 19);
                        ttsText = ttsText.replaceAll("\0","");
                        showToast(ttsText, Toast.LENGTH_SHORT);
                        mTts.speak(ttsText, TextToSpeech.QUEUE_FLUSH, null);
                    }
                    
                    break;                    
                    
                case BTCommunicator.VIBRATE_PHONE:
                    if (myBTCommunicator != null) {
                        byte[] vibrateMessage = myBTCommunicator.getReturnMessage();
                        Vibrator myVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        myVibrator.vibrate(vibrateMessage[2]*10);
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
                    // Get the device MAC address and start a new bt communicator thread
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    pairing = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
                    startBTCommunicator(address);
                }
                
                break;
                
            case REQUEST_ENABLE_BT:

                // When the request to enable Bluetooth returns
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        btOnByUs = true;
                        selectNXT();
                        break;
                    case Activity.RESULT_CANCELED:
                        showToast(R.string.bt_needs_to_be_enabled, Toast.LENGTH_SHORT);
                        finish();
                        break;
                    default:
                        showToast(R.string.problem_at_connecting, Toast.LENGTH_SHORT);
                        finish();
                        break;
                }
                
                break;

            // will not be called now, since the check intent is not generated                
            case TTS_CHECK_CODE:
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    // success, create the TTS instance
                    mTts = new TextToSpeech(this, this);
                } 
                else {
                    // missing data, install it
                    Intent installIntent = new Intent();
                    installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(installIntent);
                }
                
                break;                
        }
    }

    /**
     * Initializing of the TTS engine.
     */
    public void onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mTts.setLanguage(Locale.US);
            // Try this someday for some interesting results.
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {            
                // Language data is missing or the language is not supported.
                if (mRobotType == R.id.robot_type_lejos)
                    showToast(R.string.tts_language_not_supported, Toast.LENGTH_LONG);
            } 
        } else {
            // Initialization failed.
            if (mRobotType == R.id.robot_type_lejos)
                showToast(R.string.tts_initialization_failure, Toast.LENGTH_LONG);
        }
    }

}
