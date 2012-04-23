/**
 *   Copyright 2010 Guenther Hoelzl
 *
 *   This file is part of lejosMINDdroid.
 *
 *   lejosMINDdroid is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   lejosMINDdroid is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with lejosMINDdroid.  If not, see <http://www.gnu.org/licenses/>.
**/

import lejos.nxt.*;
import lejos.robotics.navigation.*;
import lejos.nxt.comm.*;
import lejos.nxt.remote.ErrorMessages;

/**
 * This class is for talking to MINDdroid, an application for the
 * Android platform for controlling LEGO Mindstorms robots
 * via bluetooth and the built in acceleration sensor.
 * The class communicates via LCP (LEGO communication protocol) and
 * filters some commands for doing special actions. There are some
 * methods for sending extended commands back to the phone, like
 * speaking texts via TTS or for generating haptic feedback.
 */
public class MINDdroidConnector extends LCPBTResponder {
    // magic number for protocol/firmware version
    public static byte[] FIRMWARE_VERSION_LEJOSMINDDROID = { 0x6c, 0x4d, 0x49, 0x64 };

    // extended LCP commands
    public static final byte SAY_TEXT = 0x30;
    public static final byte VIBRATE_PHONE = 0x31;
    public static final byte ACTION_BUTTON = 0x32;

    // number of self defined commands
    public final static int MAX_ADDITIONAL_COMMANDS = 10;

    // additional commands from MINDdroid
    public final static int OUTPUT_COMMAND = MAX_ADDITIONAL_COMMANDS;
    public final static int ACTION_COMMAND = MAX_ADDITIONAL_COMMANDS+1;
    public final static int DAEMON_1 = MAX_ADDITIONAL_COMMANDS+2;
    public final static int DAEMON_2 = MAX_ADDITIONAL_COMMANDS+3;
    public final static int DAEMON_3 = MAX_ADDITIONAL_COMMANDS+4;
    private final static int MAX_COMMANDS = MAX_ADDITIONAL_COMMANDS+5;

    private CommandPerformer myCommander;
    private String[] myCommands = new String[MAX_COMMANDS];
    private Thread[] myThreads = new Thread[MAX_COMMANDS];

    /**
     * Constructor
     * @param commander The object which generated the MINDdroidConnector
     */
    public MINDdroidConnector(CommandPerformer commander) {
        myCommander = commander;
    }

    /**
     * Method called with a newly read command, before it is processed.
     * Default action is to detect invalid commands and if detected to drop the
     * connection. Don't disconnect on START_PROGRAM message!
     * @param inMsg Newly read command
     * @param len length of the command
     * @return the length of the command
     */
    protected int preCommand(byte[] inMsg, int len)
    {
        if (len < 0)
            disconnect();
        return len;
    }

    /**
     * Processes the actual command and filters some commands to
     * do new actions. Default action is to call the LCP object
     * to emulate the command.
     * @param cmd The command bytes
     * @param cmdLen length of the command
     * @param reply bytes to send back in response to the command
     * @return length of the reply
     */
    protected int command(byte[] cmd, int cmdLen, byte[] reply) {
        int len = 3;
        boolean callLCP = true;
        int index;

        for(int i=0; i<reply.length; i++)
            reply[i] = 0;

        reply[0] = LCP.REPLY_COMMAND;;
        reply[1] = cmd[1];

        byte cmdId = cmd[1];

        switch (cmdId) {
            case LCP.SET_OUTPUT_STATE:
                if (myCommands[OUTPUT_COMMAND] == null)
                    break;

                myCommander.performCommand(OUTPUT_COMMAND, cmd);
                callLCP = false;
                break;

            case ACTION_BUTTON:
                // start a new thread for the action, so the
                // bluetooth communication isn't disturbed
                if (myThreads[ACTION_COMMAND] == null || !myThreads[ACTION_COMMAND].isAlive())
                    startThreadForCommand(ACTION_COMMAND);
                callLCP = false;
                break;

            case LCP.START_PROGRAM:
                // search for the right program_name
                String fileName = new String(cmd, 2, 20);
                fileName = fileName.substring(0, fileName.indexOf(0, 0));
                index = 0;
                boolean found = false;
                do {
                    // compare strings
                    if ((myCommands[index] != null) && (myCommands[index].length() == fileName.length())) {
                        found = true;
                        for (int pos = 0; pos < fileName.length(); pos++) {
                            if (fileName.charAt(pos) != myCommands[index].charAt(pos)) {
                                found = false;
                                break;
                            }
                        }
                        if (found)
                            break;
                    }
                    index++;
                } while (index < MAX_ADDITIONAL_COMMANDS);

                if (found) {
                    // stop the thread if it is already running
                    if ((myThreads[index] != null) && myThreads[index].isAlive())
                        myThreads[index].interrupt();
                    else
                        startThreadForCommand(index);
                }
                callLCP = false;
                break;

            case LCP.GET_CURRENT_PROGRAM_NAME:
                // return a false value since more than one program can
                // run simultaneously ;-)
                len = 23;
                reply[2] = 1;
                callLCP = false;
                break;

            case LCP.FIND_FIRST:
            case LCP.FIND_NEXT:
                // return the next registered method
                len = 28;
                index = (cmdId == LCP.FIND_FIRST) ? 0 : cmd[2];
                index = findNextCommand(index);
                if (index < 0) {
                    reply[2] = ErrorMessages.FILE_NOT_FOUND;
                } else {
                    for (int i=0; i<myCommands[index].length(); i++) {
                        reply[4+i] = (byte) myCommands[index].charAt(i);
                    }
                    reply[3] = (byte) (index+1);
                }
                callLCP = false;
                break;

            case LCP.GET_FIRMWARE_VERSION:
                reply[3] = FIRMWARE_VERSION_LEJOSMINDDROID[0];
                reply[4] = FIRMWARE_VERSION_LEJOSMINDDROID[1];
                reply[5] = FIRMWARE_VERSION_LEJOSMINDDROID[2];
                reply[6] = FIRMWARE_VERSION_LEJOSMINDDROID[3];
                len = 7;
                callLCP = false;
                break;
        }

        if (callLCP)
            return LCP.emulateCommand(cmd, cmdLen, reply);
        else
            return len;
    }

    /**
     * Insert a new command in a list for subsequent
     * starting via MINDdroid.
     * @param commandName the name of the new command
     * @param commandNr the position where to store the new commmand
     */
    public void registerCommand(String commandName, int commandNr) {
        if ((commandNr >= 0) && (commandNr < MAX_COMMANDS)) {
            if (commandName.length() > 19)
                commandName = commandName.substring(0, 19);
            myCommands[commandNr] = commandName;
        }
    }

    /**
     * Returns the next index where a command was inserted
     * @param index starting index for searching
     * @return found index, -1 if not found
     */
    private int findNextCommand(int index) {
        // check range of index
        if ((index < 0) || (index >= MAX_ADDITIONAL_COMMANDS)) {
            return -1;
        }

        do {
            if (myCommands[index] != null)
                return index;
            else
                index++;
        } while (index < MAX_ADDITIONAL_COMMANDS);
        return -1;
    }

    /**
     * Starts a new command as a thread.
     * @param programNr index of the program/command to start
     */
    public void startThreadForCommand(int programNr) {
        myThreads[programNr] = new CommandThread(myCommander, programNr);
        myThreads[programNr].start();
    }

    /**
     * Sends a text to the mobile phone for getting spoken via TTS
     * @param textToSay the text, at maximum 18 characters
     * @param language 0: English, 1: local language
     * @param pitch 0: female, 1: male
     * @param speechRate 0: normal, 1: fast, 2: slow
     * @param timeToWait time in ms before returning
     */
    public boolean sendTTS(String textToSay, int language, int pitch, int speechRate, int timeToWait) {
        if (conn != null) {
            byte[] message = new byte[22];
            message[0] = LCP.DIRECT_COMMAND_NOREPLY;
            message[1] = SAY_TEXT;
            // control byte
            message[2] = (byte) (speechRate & 0x0f);
            if (language != 0)
                message[2] |= (byte) 0x80;
            if (pitch != 0)
                message[2] |= (byte) 0x40;
            // cut text and copy it with 0 delimiter
            if (textToSay.length() > 18)
                textToSay = textToSay.substring(0, 18);

            for (int pos=0; pos<textToSay.length(); pos++)
                message[3+pos] = (byte) textToSay.charAt(pos);

            message[textToSay.length()+3] = 0;
            conn.write(message, message.length);
            return LMDutils.interruptedSleep(timeToWait);
        }
        return false;
    }

    /**
     * Sends a vibrate message to the phone
     * @param time in ms (allowed values 10-2550)
     */
    public void vibratePhone(int time) {
        if (conn != null) {
            byte[] message = new byte[3];
            message[0] = LCP.DIRECT_COMMAND_NOREPLY;
            message[1] = VIBRATE_PHONE;
            // control byte
            message[2] = (byte) (time/10);
            conn.write(message, message.length);
        }
    }

    /**
     * Terminates the daemon and all of its threads by calling
     * their interrupt() method.
     */
    public void terminate() {
        // interrupt all other threads
        for (int index=0; index<MAX_COMMANDS; index++) {
            if ((myThreads[index] != null) && myThreads[index].isAlive())
                myThreads[index].interrupt();
        }

        // shutdown() isn't called because it's synchronized with waitConnect()

        running = false;
        // Abort any listening operation, or in progress read
        if (conn == null)
            super.interrupt();
        else
            disconnect();
    }

    /**
     * Retrieves the connection status to MINDdroid
     * @return wheter the object is connected to MINDdroid
     */
    public boolean isConnected() {
        return conn != null;
    }
}

/**
 * This class is for starting a new command as a thread.
 */
class CommandThread extends Thread {

    private int programNr;
    private CommandPerformer commandPerformer;

    /**
     * Constructor
     * @param commandPerformer on which object to call the program
     * @param programNr number of the program to start
     */
    public CommandThread(CommandPerformer commandPerformer, int programNr) {
        this.commandPerformer = commandPerformer;
        this.programNr = programNr;
    }

    @Override
    public void run() {
        if (commandPerformer != null)
            commandPerformer.performCommand(programNr, null);
    }

}
