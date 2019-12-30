/*
 * Copyright 2010, 2011, 2012 Guenther Hoelzl, Shawn Brown
 * <p>
 * This file is part of MINDdroid.
 * <p>
 * MINDdroid is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * MINDdroid is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * MINDdroid. If not, see <http://www.gnu.org/licenses/>.
 **/
package com.lego.minddroid

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.*

/**
 * This class is for talking to a LEGO NXT robot and controlling it
 * via bluetooth and the built in acceleration sensor.
 * The communciation to the robot is done via LCP (LEGO communication protocol),
 * so no special software has to be installed on the robot.
 */
class MINDdroid : AppCompatActivity(), BTConnectable, OnInitListener {

    private var myBTCommunicator: BTCommunicator? = null
    /**
     * Gets the current connection status.
     *
     * @return the current connection status to the robot.
     */
    var isConnected = false
        private set
    private var connectingProgressDialog: ProgressDialog? = null
    private var btcHandler: Handler? = null
    private var myMenu: Menu? = null
    private var mView: GameView? = null
    private var btErrorPending = false
    private var pairing = false
    @JvmField
    var mRobotType = 0
    var motorLeft = 0
    private var directionLeft = 0// +/- 1 = 0
    var motorRight = 0
    private var stopAlreadySent = false
    private var directionRight = 0 // +/- 1 = 0
    private var motorAction = 0
    private var directionAction = 0 // +/- 1 = 0
    private var programList: MutableList<String>? = null
    private var programToStart: String? = null
    // experimental TTS support
    private lateinit var tts: TextToSpeech

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRobotType = intent.getIntExtra(SplashMenu.MINDDROID_ROBOT_TYPE, R.id.robot_type_shooterbot)
        setUpByType()
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val mySound = StartSound(this)
        mySound.start()
        // setup our view, give it focus and display.
        mView = GameView(applicationContext, this)
        mView!!.isFocusable = true
        setContentView(mView)
        // experimental TTS support for the lejosMINDdroid project
        tts = TextToSpeech(this, this)
    }

    /**
     * @return true, when currently pairing
     */
    override fun isPairing() = pairing

    /**
     * Initialization of the motor commands for the different robot types.
     */
    private fun setUpByType() {
        if (mRobotType == R.id.robot_type_tribot) {
            motorLeft = BTCommunicator.MOTOR_B
            directionLeft = 1
            motorRight = BTCommunicator.MOTOR_C
            directionRight = 1
            motorAction = BTCommunicator.MOTOR_A
            directionAction = 1
        } else if (mRobotType == R.id.robot_type_robogator) {
            motorLeft = BTCommunicator.MOTOR_C
            directionLeft = -1
            motorRight = BTCommunicator.MOTOR_B
            directionRight = -1
            motorAction = BTCommunicator.MOTOR_A
            directionAction = 1
        } else { // default
            motorLeft = BTCommunicator.MOTOR_B
            directionLeft = 1
            motorRight = BTCommunicator.MOTOR_C
            directionRight = 1
            motorAction = BTCommunicator.MOTOR_A
            directionAction = 1
        }
    }

    /**
     * Updates the menus and possible buttons when connection status changed.
     */
    private fun updateButtonsAndMenu() {
        if (myMenu == null) return
        myMenu!!.removeItem(MENU_TOGGLE_CONNECT)
        if (isConnected) {
            myMenu!!.add(0, MENU_TOGGLE_CONNECT, 1, resources.getString(R.string.disconnect)).setIcon(R.drawable.ic_menu_connected)
        } else {
            myMenu!!.add(0, MENU_TOGGLE_CONNECT, 1, resources.getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect)
        }
    }

    /**
     * Creates a new object for communication to the NXT robot via bluetooth and fetches the corresponding handler.
     */
    private fun createBTCommunicator() { // interestingly BT adapter needs to be obtained by the UI thread - so we pass it in in the constructor
        myBTCommunicator = BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter(), resources)
        btcHandler = myBTCommunicator!!.handler
    }

    /**
     * Creates and starts the a thread for communication via bluetooth to the NXT robot.
     *
     * @param mac_address The MAC address of the NXT robot.
     */
    private fun startBTCommunicator(mac_address: String?) {
        isConnected = false
        connectingProgressDialog = ProgressDialog.show(this, "", resources.getString(R.string.connecting_please_wait), true)
        if (myBTCommunicator != null) {
            try {
                myBTCommunicator!!.destroyNXTconnection()
            } catch (ignored: IOException) {
            }
        }
        createBTCommunicator()
        myBTCommunicator!!.setMACAddress(mac_address)
        myBTCommunicator!!.start()
        updateButtonsAndMenu()
    }

    /**
     * Sends a message for disconnecting to the communcation thread.
     */
    fun destroyBTCommunicator() {
        if (myBTCommunicator != null) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT, 0, 0)
            myBTCommunicator = null
        }
        isConnected = false
        updateButtonsAndMenu()
    }

    /**
     * Does something special depending on the robot-type.
     *
     * @param buttonMode short, long or other press types.
     */
    private fun performActionCommand(buttonMode: Int) {
        if (mRobotType != R.id.robot_type_lejos) {
            if (buttonMode == ACTION_BUTTON_SHORT) { // Wolfgang Amadeus Mozart
// "Zauberfloete - Der Vogelfaenger bin ich ja"
                sendBTCmessage(BTCommunicator.NO_DELAY,
                        BTCommunicator.DO_BEEP, 392, 100)
                sendBTCmessage(200, BTCommunicator.DO_BEEP, 440, 100)
                sendBTCmessage(400, BTCommunicator.DO_BEEP, 494, 100)
                sendBTCmessage(600, BTCommunicator.DO_BEEP, 523, 100)
                sendBTCmessage(800, BTCommunicator.DO_BEEP, 587, 300)
                sendBTCmessage(1200, BTCommunicator.DO_BEEP, 523, 300)
                sendBTCmessage(1600, BTCommunicator.DO_BEEP, 494, 300)
            } else { // G-F-E-D-C
                sendBTCmessage(BTCommunicator.NO_DELAY,
                        BTCommunicator.DO_BEEP, 392, 100)
                sendBTCmessage(200, BTCommunicator.DO_BEEP, 349, 100)
                sendBTCmessage(400, BTCommunicator.DO_BEEP, 330, 100)
                sendBTCmessage(600, BTCommunicator.DO_BEEP, 294, 100)
                sendBTCmessage(800, BTCommunicator.DO_BEEP, 262, 300)
            }
        }
        // MOTOR ACTION: forth an back
        if (mRobotType == R.id.robot_type_robogator) { // Robogator: bite the user in any case ;-)
            for (bite in 0..2) {
                sendBTCmessage(bite * 400, motorAction, 75 * directionAction, 0)
                sendBTCmessage(bite * 400 + 200, motorAction, -75 * directionAction, 0)
            }
            sendBTCmessage(3 * 400, motorAction, 0, 0)
        } else if (mRobotType == R.id.robot_type_lejos) { // lejosMINDdroid: just send the message for button press
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.DO_ACTION, buttonMode, 0)
        } else { // other robots: 180 degrees forth and back
            val direction = if (buttonMode == ACTION_BUTTON_SHORT) 1 else -1
            sendBTCmessage(BTCommunicator.NO_DELAY, motorAction, 75 * direction * directionAction, 0)
            sendBTCmessage(500, motorAction, -75 * direction * directionAction, 0)
            sendBTCmessage(1000, motorAction, 0, 0)
        }
    }

    /**
     * Method for performing the appropriate action when the ACTION button is pressed shortly.
     */
    fun actionButtonPressed() {
        if (myBTCommunicator != null) {
            mView!!.thread.mActionPressed = true
            performActionCommand(ACTION_BUTTON_SHORT)
        }
    }

    /**
     * Method for performing the appropriate action when the ACTION button is long pressed.
     */
    fun actionButtonLongPress() {
        if (myBTCommunicator != null) {
            mView!!.thread.mActionPressed = true
            performActionCommand(ACTION_BUTTON_LONG)
        }
    }

    /**
     * Starts a program on the NXT robot.
     *
     * @param name The program name to start. Has to end with .rxe on the LEGO firmware and with .nxj on the
     * leJOS NXJ firmware.
     */
    fun startProgram(name: String) { // for .rxe programs: get program name, eventually stop this and start the new one delayed
// is handled in startRXEprogram()
        if (name.endsWith(".rxe")) {
            programToStart = name
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_PROGRAM_NAME, 0, 0)
            return
        }
        // for .nxj programs: stop bluetooth communication after starting the program
        if (name.endsWith(".nxj")) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, name)
            destroyBTCommunicator()
            return
        }
        // for all other programs: just start the program
        sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, name)
    }

    /**
     * Depending on the status (whether the program runs already) we stop it, wait and restart it again.
     *
     * @param status The current status, 0x00 means that the program is already running.
     */
    fun startRXEprogram(status: Byte) {
        if (status.toInt() == 0x00) {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.STOP_PROGRAM, 0, 0)
            sendBTCmessage(1000, BTCommunicator.START_PROGRAM, programToStart)
        } else {
            sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.START_PROGRAM, programToStart)
        }
    }

    /**
     * Sends the motor control values to the communcation thread.
     *
     * @param left The power of the left motor from 0 to 100.
     */
    fun updateMotorControl(left: Int, right: Int) {
        if (myBTCommunicator != null) { // don't send motor stop twice
            stopAlreadySent = if (left == 0 && right == 0) {
                if (stopAlreadySent) return else true
            } else false
            // send messages via the handler
            sendBTCmessage(BTCommunicator.NO_DELAY, motorLeft, left * directionLeft, 0)
            sendBTCmessage(BTCommunicator.NO_DELAY, motorRight, right * directionRight, 0)
        }
    }

    /**
     * Sends the message via the BTCommuncator to the robot.
     *
     * @param delay   time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     * @param value1  first parameter
     * @param value2  second parameter
     */
    fun sendBTCmessage(delay: Int, message: Int, value1: Int, value2: Int) {
        val myBundle = Bundle()
        myBundle.putInt("message", message)
        myBundle.putInt("value1", value1)
        myBundle.putInt("value2", value2)
        val myMessage = myHandler.obtainMessage()
        myMessage.data = myBundle
        if (delay == 0) btcHandler!!.sendMessage(myMessage) else btcHandler!!.sendMessageDelayed(myMessage, delay.toLong())
    }

    /**
     * Sends the message via the BTCommuncator to the robot.
     *
     * @param delay   time to wait before sending the message.
     * @param message the message type (as defined in BTCommucator)
     */
    fun sendBTCmessage(delay: Int, message: Int, name: String?) {
        val myBundle = Bundle()
        myBundle.putInt("message", message)
        myBundle.putString("name", name)
        val myMessage = myHandler.obtainMessage()
        myMessage.data = myBundle
        if (delay == 0) btcHandler!!.sendMessage(myMessage) else btcHandler!!.sendMessageDelayed(myMessage, delay.toLong())
    }

    public override fun onResume() {
        super.onResume()
        try {
            mView!!.registerListener()
        } catch (ex: IndexOutOfBoundsException) {
            showToast(R.string.sensor_initialization_failure)
            destroyBTCommunicator()
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        // no bluetooth available
        if (BluetoothAdapter.getDefaultAdapter() == null) {
            showToast(R.string.bt_initialization_failure)
            destroyBTCommunicator()
            finish()
            return
        }
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            selectNXT()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        destroyBTCommunicator()
    }

    public override fun onPause() {
        super.onPause()
        mView!!.unregisterListener()
        destroyBTCommunicator()
        super.onStop()
    }

    public override fun onSaveInstanceState(icicle: Bundle) {
        super.onSaveInstanceState(icicle)
        mView!!.unregisterListener()
    }

    /**
     * Creates the menu items
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        myMenu = menu
        myMenu!!.add(0, MENU_TOGGLE_CONNECT, 1, resources.getString(R.string.connect)).setIcon(R.drawable.ic_menu_connect)
        myMenu!!.add(0, MENU_START_SW, 2, resources.getString(R.string.start)).setIcon(R.drawable.ic_menu_start)
        myMenu!!.add(0, MENU_QUIT, 3, resources.getString(R.string.quit)).setIcon(R.drawable.ic_menu_exit)
        updateButtonsAndMenu()
        return true
    }

    /**
     * Enables/disables the menu items
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val displayMenu: Boolean
        displayMenu = super.onPrepareOptionsMenu(menu)
        if (displayMenu) {
            var startEnabled = false
            if (myBTCommunicator != null) startEnabled = myBTCommunicator!!.isConnected
            menu.findItem(MENU_START_SW).isEnabled = startEnabled
        }
        return displayMenu
    }

    /**
     * Handles item selections
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_TOGGLE_CONNECT -> {
                if (myBTCommunicator == null || !isConnected) {
                    selectNXT()
                } else {
                    destroyBTCommunicator()
                    updateButtonsAndMenu()
                }
                return true
            }
            MENU_START_SW -> {
                if (programList!!.size == 0) {
                    showToast(R.string.no_programs_found)
                }
                val myFileDialog = FileDialog(this, programList)
                myFileDialog.show(mRobotType == R.id.robot_type_lejos)
                return true
            }
            MENU_QUIT -> {
                destroyBTCommunicator()
                finish()
                if (btOnByUs) showToast(R.string.bt_off_message)
                SplashMenu.quitApplication()
                return true
            }
        }
        return false
    }

    private fun showToast(textToShow: String?) {
        Toast.makeText(this, textToShow, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(resID: Int) {
        Toast.makeText(this, resID, Toast.LENGTH_SHORT).show()
    }

    /**
     * Receive messages from the BTCommunicator
     */
    val myHandler: Handler = object : Handler() {
        override fun handleMessage(myMessage: Message) {
            when (myMessage.data.getInt("message")) {
                BTCommunicator.DISPLAY_TOAST -> showToast(myMessage.data.getString("toastText"))
                BTCommunicator.STATE_CONNECTED -> {
                    isConnected = true
                    programList = ArrayList()
                    connectingProgressDialog!!.dismiss()
                    updateButtonsAndMenu()
                    sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.GET_FIRMWARE_VERSION, 0, 0)
                }
                BTCommunicator.MOTOR_STATE -> if (myBTCommunicator != null) {
                    val motorMessage = myBTCommunicator!!.returnMessage
                    val position = ByteHelper.byteToInt(motorMessage[21]) +
                            (ByteHelper.byteToInt(motorMessage[22]) shl 8) +
                            (ByteHelper.byteToInt(motorMessage[23]) shl 16) +
                            (ByteHelper.byteToInt(motorMessage[24]) shl 24)
                    showToast(resources.getString(R.string.current_position) + position)
                }
                BTCommunicator.STATE_CONNECTERROR_PAIRING -> {
                    connectingProgressDialog!!.dismiss()
                    destroyBTCommunicator()
                }
                BTCommunicator.STATE_CONNECTERROR -> {
                    connectingProgressDialog!!.dismiss()
                    destroyBTCommunicator()
                    if (!btErrorPending) {
                        btErrorPending = true
                        // inform the user of the error with an AlertDialog
                        val builder = AlertDialog.Builder(this@MINDdroid)
                        builder.setTitle(resources.getString(R.string.bt_error_dialog_title))
                                .setMessage(resources.getString(R.string.bt_error_dialog_message)).setCancelable(false)
                                .setPositiveButton("OK") { dialog: DialogInterface, id: Int ->
                                    btErrorPending = false
                                    dialog.cancel()
                                    selectNXT()
                                }
                        builder.create().show()
                    }
                }
                BTCommunicator.STATE_RECEIVEERROR, BTCommunicator.STATE_SENDERROR -> {
                    destroyBTCommunicator()
                    if (!btErrorPending) {
                        btErrorPending = true
                        val builder = AlertDialog.Builder(this@MINDdroid)
                        builder.setTitle(resources.getString(R.string.bt_error_dialog_title))
                                .setMessage(resources.getString(R.string.bt_error_dialog_message)).setCancelable(false)
                                .setPositiveButton("OK") { dialog: DialogInterface, id: Int ->
                                    btErrorPending = false
                                    dialog.cancel()
                                    selectNXT()
                                }
                        builder.create().show()
                    }
                }
                BTCommunicator.FIRMWARE_VERSION -> if (myBTCommunicator != null) {
                    val firmwareMessage = myBTCommunicator!!.returnMessage
                    // check if we know the firmware
                    var isLejosMindDroid = true
                    var pos = 0
                    while (pos < 4) {
                        if (firmwareMessage[pos + 3] != LCPMessage.FIRMWARE_VERSION_LEJOSMINDDROID[pos]) {
                            isLejosMindDroid = false
                            break
                        }
                        pos++
                    }
                    if (isLejosMindDroid) {
                        mRobotType = R.id.robot_type_lejos
                        setUpByType()
                    }
                    // afterwards we search for all files on the robot
                    sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES, 0, 0)
                }
                BTCommunicator.FIND_FILES -> if (myBTCommunicator != null) {
                    val fileMessage = myBTCommunicator!!.returnMessage
                    var fileName = String(fileMessage, 4, 20)
                    fileName = fileName.replace("\u0000".toRegex(), "")
                    if (mRobotType == R.id.robot_type_lejos || fileName.endsWith(".nxj") || fileName.endsWith(".rxe")) {
                        programList!!.add(fileName)
                    }
                    // find next entry with appropriate handle,
// limit number of programs (in case of error (endless loop))
                    if (programList!!.size <= MAX_PROGRAMS) sendBTCmessage(BTCommunicator.NO_DELAY, BTCommunicator.FIND_FILES, 1, ByteHelper.byteToInt(fileMessage[3]))
                }
                BTCommunicator.PROGRAM_NAME -> if (myBTCommunicator != null) {
                    val returnMessage = myBTCommunicator!!.returnMessage
                    startRXEprogram(returnMessage[2])
                }
                BTCommunicator.SAY_TEXT -> if (myBTCommunicator != null) {
                    val resultText = ByteHelper.handleResult(tts, myBTCommunicator!!.returnMessage)
                    showToast(resultText)
                    tts.speak(resultText, TextToSpeech.QUEUE_FLUSH, null,  null)
                }
                BTCommunicator.VIBRATE_PHONE -> if (myBTCommunicator != null) {
                    val vibrateMessage = myBTCommunicator!!.returnMessage
                    val myVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    myVibrator.vibrate(vibrateMessage[2] * 10.toLong())
                }
            }
        }
    }

    fun selectNXT() {
        val serverIntent = Intent(this, DeviceListActivity::class.java)
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CONNECT_DEVICE ->  // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) { // Get the device MAC address and start a new bt communicator thread
                    val address = data!!.extras!!.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
                    pairing = data.extras!!.getBoolean(DeviceListActivity.PAIRING)
                    startBTCommunicator(address)
                }
            REQUEST_ENABLE_BT -> when (resultCode) {
                Activity.RESULT_OK -> {
                    btOnByUs = true
                    selectNXT()
                }
                Activity.RESULT_CANCELED -> {
                    showToast(R.string.bt_needs_to_be_enabled)
                    finish()
                }
                else -> {
                    showToast(R.string.problem_at_connecting)
                    finish()
                }
            }
            TTS_CHECK_CODE -> if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) { // success, create the TTS instance
                tts = TextToSpeech(this, this)
            } else { // missing data, install it
                val installIntent = Intent()
                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                startActivity(installIntent)
            }
        }
    }

    /**
     * Initializing of the TTS engine.
     */
    override fun onInit(status: Int) { // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) { // Set preferred language to US english.
// Note that a language may not be available, and the result will indicate this.
            val result = tts.setLanguage(Locale.US)
            // Try this someday for some interesting results.
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) { // Language data is missing or the language is not supported.
                if (mRobotType == R.id.robot_type_lejos) showToast(R.string.tts_language_not_supported)
            }
        } else { // Initialization failed.
            if (mRobotType == R.id.robot_type_lejos) showToast(R.string.tts_initialization_failure)
        }
    }

    companion object {

        private const val TTS_CHECK_CODE = 9991
        const val UPDATE_TIME = 200
        const val MENU_TOGGLE_CONNECT = Menu.FIRST
        const val MENU_START_SW = Menu.FIRST + 1
        const val MENU_QUIT = Menu.FIRST + 2
        const val ACTION_BUTTON_SHORT = 0
        const val ACTION_BUTTON_LONG = 1
        private const val REQUEST_CONNECT_DEVICE = 1000
        private const val REQUEST_ENABLE_BT = 2000
        private var btOnByUs = false
        private const val MAX_PROGRAMS = 20
        /**
         * Asks if bluetooth was switched on during the runtime of the app. For saving
         * battery we switch it off when the app is terminated.
         *
         * @return true, when bluetooth was switched on by the app
         */
        @JvmStatic
        fun isBtOnByUs(): Boolean {
            return btOnByUs
        }

        /**
         * Sets a flag when bluetooth was switched on durin runtime
         *
         * @param btOnByUs true, when bluetooth was switched on by the app
         */
        @JvmStatic
        fun setBtOnByUs(btOnByUs: Boolean) {
            Companion.btOnByUs = btOnByUs
        }
    }
}