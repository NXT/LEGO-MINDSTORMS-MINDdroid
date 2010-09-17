package com.lego.minddroid;

/*
  LCPMessage is a class for composing the proper messages

  This file is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
*/


/**
 * Class for composing the proper messages for simple 
 * communication over bluetooth
 */
public class LCPMessage {


    public static byte[] getBeepMessage(int frequency, int duration) {
        byte[] message = new byte[8];
    
        // message length    
        message[0] = 6;
        message[1] = 0;
        // Direct command telegram, no response
        message[2] = (byte) 0x80;
        message[3] = (byte) 0x03;
        // Frequency for the tone, Hz (UWORD); Range: 200-14000 Hz
        message[4] = (byte) (frequency & 0xff);
        message[5] = (byte) (frequency >> 8);
        // Duration of the tone, ms (UWORD)
        message[6] = (byte) (duration & 0xff);
        message[7] = (byte) (duration >> 8);

        return message;
    }


    public static byte[] getMotorMessage(int motor, int speed) {
        byte[] message = new byte[14];

        // message length          
        message[0] = 12;
        message[1] = 0;
        // Direct command telegram, no response        
        message[2] = (byte) 0x80;
        message[3] = (byte) 0x04;
        // Output port
        message[4] = (byte)(motor & 0xff);
        if (speed == 0) {
            message[5] = 0;
            message[6] = 0;
            message[7] = 0;
            message[8] = 0;
            message[9] = 0;
        }
        else {
            // Power set option (Range: -100 - 100)
            message[5] = (byte) speed;
            // Mode byte (Bit-field): MOTORON
            message[6] = 0x01;
            // Regulation mode: REGULATION_MODE_MOTOR_SPEED
            message[7] = 0x01;
            // Turn Ratio (SBYTE; -100 - 100)
            message[8] = 0x00;
            // RunState: MOTOR_RUN_STATE_RUNNING
            message[9] = 0x20;
        }

        // TachoLimit: run forever
        message[10] = 0;
        message[11] = 0;
        message[12] = 0;
        message[13] = 0;

        return message;

    }

    // this message doesn't work correctly at the moment on the NXT
    public static byte[] getMotorMessage(int motor, int speed, int end) {
        byte[] message = getMotorMessage(motor, speed);
        // TachoLimit
        message[10] = (byte) (end & 0xff);
        message[11] = (byte) ((end >> 8) & 0xff);
        message[12] = (byte) ((end >> 16) & 0xff);
        message[13] = (byte) ((end >> 24) & 0xff);

        return message;
    }


    public static byte[] getResetMessage(int motor) {
        byte[] message = new byte[6];
    
        // message length    
        message[0] = 4;
        message[1] = 0;
        // Direct command telegram, no response
        message[2] = (byte) 0x80;
        message[3] = (byte) 0x0A;
        // Output port
        message[4] = (byte)(motor & 0xff);
        // absolute position
        message[5] = 0;

        return message;
    }

}
