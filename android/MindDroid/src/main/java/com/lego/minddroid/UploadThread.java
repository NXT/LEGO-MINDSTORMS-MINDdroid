/*
 *   Copyright 2011, 2012 Guenther Hoelzl
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

/**
 * The tasks have to be done in this thread, so the user interface
 * isn't blocked.
 */
public final class UploadThread extends Thread {

    // errorcodes during the task
    static final int NO_ERROR = 0;
    static final int OPEN_BT_ERROR = 1;
    static final int CLOSE_BT_ERROR = 2;
    static final int OPEN_FILE_ERROR = 3;
    static final int UPLOAD_ERROR = 4;

    // status of the thread
    private static final int IDLE = 0;
    static final int CONNECTING = 1;
    static final int UPLOADING = 2;

    private static final int MAX_BUFFER_SIZE = 58;

    private Handler handler;

    private UploadThreadListener listener;

    private Resources resources;

    private BTCommunicator mBTCommunicator;

    private int mFileLength;

    private int mUploaded;

    private int errorCode;

    UploadThread(UploadThreadListener listener, Resources resources) {
        this.listener = listener;
        this.resources = resources;
    }

    void setBluetoothCommunicator(BTCommunicator communicator) {
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
        } catch (Throwable ignored) {
        }
    }

    /**
     * Puts a new request for stopping the looper into the queue
     */
    synchronized void requestStop() {
        handler.post(() -> Looper.myLooper().quit());
    }

    /**
     * Puts a new request for uploading into the queue handled by the looper
     * @param fileName the name of the file to upload including the path
     */
    synchronized void enqueueUpload(final String nxtAddress, final String fileName) {
        handler.post(() -> {
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
        });
    }

    /**
     * @return the maximum number of bytes to be uploaded
     */
    int getFileLength() {
        return mFileLength;
    }

    /**
     * @return the number of bytes already uploaded
     */
    int getBytesUploaded() {
        return mUploaded;
    }

    /**
     * @return the error after an action
     */
    int getErrorCode() {
        return errorCode;
    }

    /**
     * Resets the error status
     */
    void resetErrorCode() {
        errorCode = NO_ERROR;
    }

    /**
     * Opens a file with the given filename and uplodads it to the robot
     * @param fileName the name of the file to upload including the path
     */
    private void uploadFile(String fileName) throws IOException {
        byte[] data = new byte[MAX_BUFFER_SIZE];
        int readLength;
        InputStream inputStream;
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
        fileName = fileName.substring(lastSlashPos + 1);
        
        boolean triedDelete = false;
        while (true) {
            // send OpenWriteMessage
            message = LCPMessage.getOpenWriteMessage(fileName, mFileLength);
            mBTCommunicator.sendMessage(message);
            // get reply message including handle
            message = mBTCommunicator.receiveMessage();
            // check message if everything's OK
            if (message != null &&
                message.length == 4 &&
                message[0] == LCPMessage.REPLY_COMMAND &&
                message[1] == LCPMessage.OPEN_WRITE &&
                message[2] == 0)
                break;
            
            // file exists => try to delete file only once
            if (!triedDelete &&
                message != null &&
                message.length > 2 &&
                message[0] == LCPMessage.REPLY_COMMAND &&
                message[1] == LCPMessage.OPEN_WRITE &&
                message[2] == (byte)0x8f) {
                triedDelete = true;
                message = LCPMessage.getDeleteMessage(fileName);
                mBTCommunicator.sendMessage(message);            
                message = mBTCommunicator.receiveMessage();
                continue;                
            }
            else {
                throw new IOException();
            }
        }

        byte handle = message[3];
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
