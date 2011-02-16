/**
 *   Copyright 2011 Guenther Hoelzl
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

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import java.io.*;

/**
 * The tasks have to be done in this thread, so the user interface
 * isn't blocked.
 */
public final class UploadThread extends Thread {

    // errorcodes during the task
    public static final int NO_ERROR = 0;
    public static final int OPEN_BT_ERROR = 1;
    public static final int CLOSE_BT_ERROR = 2;
    public static final int OPEN_FILE_ERROR = 3;
    public static final int UPLOAD_ERROR = 4;

    // status of the thread
    public static final int IDLE = 0;
    public static final int CONNECTING = 1;
    public static final int UPLOADING = 2;

    private static final int MAX_BUFFER_SIZE = 58;

    private Handler handler;

    private UploadThreadListener listener;

    private Resources resources;

    private BTCommunicator mBTCommunicator;

    private int mFileLength;

    private int mUploaded;

    private int errorCode;

    public UploadThread(UploadThreadListener listener, Resources resources) {
        this.listener = listener;
        this.resources = resources;
    }

    public void setBluetoothCommunicator(BTCommunicator communicator) {
        mBTCommunicator = communicator;
    }

    /**
     * Prepares and starts the looper of the thread
     */
    @Override
    public void run() {
        try {
            Looper.prepare();
            handler = new Handler();
            Looper.loop();
        } catch (Throwable t) {
        }
    }

    /**
     * Puts a new request for stopping the looper into the queue
     */
    public synchronized void requestStop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Looper.myLooper().quit();
            }
        });
    }

    /**
     * Puts a new request for uploading into the queue handled by the looper
     * @param fileName the name of the file to upload including the path
     */
    public synchronized void enqueueUpload(final String nxtAddress, final String fileName) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean uploading = false;
                try {
                    signalUpdate(CONNECTING);
                    mBTCommunicator.setMACAddress(nxtAddress);
                    mBTCommunicator.createNXTconnection();
                    signalUpdate(UPLOADING);
                    uploading = true;
                    uploadFile(fileName);
                    signalUpdate(IDLE);
                } catch (FileNotFoundException e) {
                    errorCode = OPEN_FILE_ERROR;
                } catch (IOException e) {
                    errorCode = uploading ? UPLOAD_ERROR : OPEN_BT_ERROR;
                } finally {
                    try {
                        mBTCommunicator.destroyNXTconnection();
                    } catch (IOException e) {
                        errorCode = CLOSE_BT_ERROR;
                    }
                    signalUpdate(IDLE);
                }
            }
        });
    }

    /**
     * @return the maximum number of bytes to be uploaded
     */
    public int getFileLength() {
        return mFileLength;
    }

    /**
     * @return the number of bytes already uploaded
     */
    public int getBytesUploaded() {
        return mUploaded;
    }

    /**
     * @return the error after an action
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Resets the error status
     */
    public void resetErrorCode() {
        errorCode = NO_ERROR;
    }

    /**
     * Opens a file with the given filename and uplodads it to the robot
     * @param fileName the name of the file to upload including the path
     */
    private void uploadFile(String fileName) throws FileNotFoundException, IOException {
        byte[] data = new byte[MAX_BUFFER_SIZE];
        int readLength;
        byte handle;
        InputStream inputStream = null;
        byte[] message;


        // internal file: no path given
        if (fileName.indexOf('/') == -1) {
            int dotPosition = fileName.lastIndexOf('.');
            String resourceName = fileName.substring(0, dotPosition).toLowerCase();
            int id = resources.getIdentifier(resourceName, "raw", "com.lego.minddroid");
            inputStream = resources.openRawResource(id);
            // read stream once for getting it's size and reopen it afterwards
            mFileLength = 0;
            while ((readLength = inputStream.read(data)) > 0)
                mFileLength += readLength;
            inputStream = resources.openRawResource(id);
        }
        // external file: with full path
        else {
            File file = new File(fileName);
            if (!file.exists())
                throw new FileNotFoundException();
            inputStream = new FileInputStream(file);
            mFileLength = (int) file.length();
        }

        mUploaded = 0;
        // extract fileName without path
        int lastSlashPos = fileName.lastIndexOf('/');
        fileName = fileName.substring(lastSlashPos + 1, fileName.length());

        // send OpenWriteMessage
        message = LCPMessage.getOpenWriteMessage(fileName, mFileLength);
        mBTCommunicator.sendMessage(message);
        // get reply message with handle
        message = mBTCommunicator.receiveMessage();
        // check message and get handle
        if (message == null ||
                message.length != 4 ||
                message[0] != LCPMessage.REPLY_COMMAND ||
                message[1] != LCPMessage.OPEN_WRITE ||
                message[2] != 0)
            throw new IOException();

        handle = message[3];
        while ((readLength = inputStream.read(data)) > 0) {
            // send WriteMessage and receive reply with next handle
            message = LCPMessage.getWriteMessage(handle, data, readLength);
            mBTCommunicator.sendMessage(message);
            // get reply message and with handle
            message = mBTCommunicator.receiveMessage();
            // check message and get handle
            if (message == null ||
                    message.length != 6 ||
                    message[0] != LCPMessage.REPLY_COMMAND ||
                    message[1] != LCPMessage.WRITE ||
                    message[2] != 0)
                throw new IOException();

            handle = message[3];
            mUploaded += readLength;
            signalUpdate(UPLOADING);
        }
        // send CloseFile(handle);
        message = LCPMessage.getCloseMessage(handle);
        mBTCommunicator.sendMessage(message);
        // get reply message with handle
        message = mBTCommunicator.receiveMessage();
        // check message
        if (message == null ||
                message.length != 4 ||
                message[0] != LCPMessage.REPLY_COMMAND ||
                message[1] != LCPMessage.CLOSE ||
                message[2] != 0)
            throw new IOException();
    }

    /**
     * Informs the listener activity to make an update at the screen.
     * @param status the current status of the thread
     */
    private void signalUpdate(int status) {
        if (listener != null)
            listener.handleUploadThreadUpdate(status);
    }

}
