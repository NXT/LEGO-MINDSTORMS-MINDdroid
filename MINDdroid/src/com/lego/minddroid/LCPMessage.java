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

/**
 * Class for composing the proper messages for simple
 * communication over bluetooth
 */
public class LCPMessage {


    public static byte[] getBeepMessage(int frequency, int duration) {
        byte[] message = new byte[6];

        // Direct command telegram, no response
        message[0] = (byte) 0x80;
        message[1] = (byte) 0x03;
        // Frequency for the tone, Hz (UWORD); Range: 200-14000 Hz
        message[2] = (byte) frequency;
        message[3] = (byte) (frequency >> 8);
        // Duration of the tone, ms (UWORD)
        message[4] = (byte) duration;
        message[5] = (byte) (duration >> 8);

        return message;
    }


    public static byte[] getMotorMessage(int motor, int speed) {
        byte[] message = new byte[12];

        // Direct command telegram, no response
        message[0] = (byte) 0x80;
        message[1] = (byte) 0x04;
        // Output port
        message[2] = (byte) motor;

        if (speed == 0) {
            message[3] = 0;
            message[4] = 0;
            message[5] = 0;
            message[6] = 0;
            message[7] = 0;

        } else {
            // Power set option (Range: -100 - 100)
            message[3] = (byte) speed;
            // Mode byte (Bit-field): MOTORON + BREAK
            message[4] = 0x03;
            // Regulation mode: REGULATION_MODE_MOTOR_SPEED
            message[5] = 0x01;
            // Turn Ratio (SBYTE; -100 - 100)
            message[6] = 0x00;
            // RunState: MOTOR_RUN_STATE_RUNNING
            message[7] = 0x20;
        }

        // TachoLimit: run forever
        message[8] = 0;
        message[9] = 0;
        message[10] = 0;
        message[11] = 0;

        return message;

    }


    public static byte[] getMotorMessage(int motor, int speed, int end) {
        byte[] message = getMotorMessage(motor, speed);

        // TachoLimit
        message[8] = (byte) end;
        message[9] = (byte) (end >> 8);
        message[10] = (byte) (end >> 16);
        message[11] = (byte) (end >> 24);

        return message;
    }


    public static byte[] getResetMessage(int motor) {
        byte[] message = new byte[4];

        // Direct command telegram, no response
        message[0] = (byte) 0x80;
        message[1] = (byte) 0x0A;
        // Output port
        message[2] = (byte) motor;
        // absolute position
        message[3] = 0;

        return message;
    }


    public static byte[] getProgramMessage(String programName) {
        byte[] message = new byte[22];

        // Direct command telegram, no response
        message[0] = (byte) 0x80;
        message[1] = (byte) 0x00;

        // copy programName and end with 0 delimiter
        for (int pos=0; pos<programName.length(); pos++)
            message[2+pos] = (byte) programName.charAt(pos);

        message[programName.length()+2] = 0;

        return message;
    }


    public static byte[] getOutputStateMessage(int motor) {
        byte[] message = new byte[3];

        // Direct command telegram, with response
        message[0] = (byte) 0x00;
        message[1] = (byte) 0x06;
        // Output port
        message[2] = (byte) motor;

        return message;
    }


    public static byte[] getFirmwareVersionMessage() {
        byte[] message = new byte[2];

        // System command, reply required
        message[0] = (byte) 0x01;
        message[1] = (byte) 0x88;

        return message;
    }
    
    
    public static byte[] getFindFilesMessage(boolean findFirst, int handle, String searchString) {
        byte[] message;
        
        if (findFirst)
            message = new byte[22];
        else 
            message = new byte[3];    

        // System command, reply required
        message[0] = (byte) 0x01;
        
        if (findFirst) {
            message[1] = (byte) 0x86;
            
        
            // copy searchString and end with 0 delimiter
            for (int pos=0; pos<searchString.length(); pos++)
                message[2+pos] = (byte) searchString.charAt(pos);

            message[searchString.length()+2] = 0;
        }
        else {
            message[1] = (byte) 0x87;
            message[2] = (byte) handle;
        }    

        return message;
    }


}
