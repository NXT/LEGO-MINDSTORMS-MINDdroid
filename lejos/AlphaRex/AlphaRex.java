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
import lejos.robotics.*;

/**
 * This class controls the AlphaRex from the MINDSTORMS 2.0 set
 * via MINDdroid.
 * The motors/sensors have to be connected as the following:
 * Motor on the back of the robot: PORT A
 * Motor for the right leg: PORT B
 * Motor the the left leg: PORT C
 * Touch sensor on the right leg: PORT 1
 * Touch sensor on the left leg: PORT 2
 * Ultrasonic sensor on the head: PORT 3
 * Color Sensor on the hand: PORT 4
 * (Originally USS and Color Sensor ports a reversed, but
 * because of a little bug in leJOS 0.85 the USS can't be
 * attached to PORT 4).
 */
public class AlphaRex implements CommandPerformer {
    // implemented commands
    private final static int HELLO_ROBOT = 0;
    private final static int HALLO_ROBOTER = 1;
    private final static int MUSIC_COMMAND = 2;
    private final static int USS_READER = 3;
    private final static int COLOR_GUESSER = 4;

    // Init states for moving of AlphaRex
    private final static int NOT_INIT = 0;
    private final static int INIT_FORWARD = 1;
    private final static int INIT_BACKWARD = 2;

    private static int initState = NOT_INIT;
    private static int averageWalkingPower;
    private static int walkingPower;
    private static int averageShakingPower;
    private static int shakingPower;
    private static MINDdroidConnector lcpThread;
    private static boolean colorSensorInUse = false;

    /**
     * The start of the program. Does registering of the commands.
     * @args The command line paramaters
     */
    public static void main(String[] args ) throws Exception {
        lcpThread = new MINDdroidConnector(new AlphaRex());
        lcpThread.registerCommand("Hello, robot!", HELLO_ROBOT);
        lcpThread.registerCommand("Hallo, Roboter!", HALLO_ROBOTER);
        lcpThread.registerCommand("Musette from Bach", MUSIC_COMMAND);
        lcpThread.registerCommand("Ultrasonic-Reader", USS_READER);
        lcpThread.registerCommand("Color-Guesser", COLOR_GUESSER);
        // also register the output and action command here,
        // so we can decide for ourselves what to do when
        // we get new motor commands or action button press
        // from MINDdroid
        lcpThread.registerCommand("OUTPUT", MINDdroidConnector.OUTPUT_COMMAND);
        lcpThread.registerCommand("ACTION", MINDdroidConnector.ACTION_COMMAND);
        // register the command for displaying effects and start it as a thread
        lcpThread.registerCommand("DISPLAY", MINDdroidConnector.DAEMON_1);
        lcpThread.startThreadForCommand(MINDdroidConnector.DAEMON_1);
        // register the command for shaking the robot and start it as a thread
        lcpThread.registerCommand("SHAKE", MINDdroidConnector.DAEMON_2);
        lcpThread.startThreadForCommand(MINDdroidConnector.DAEMON_2);
        // register the command for moving the robot and start it as a thread
        lcpThread.registerCommand("WALK", MINDdroidConnector.DAEMON_3);
        lcpThread.startThreadForCommand(MINDdroidConnector.DAEMON_3);

        lcpThread.setDaemon(true);
        lcpThread.start();

        boolean running = true;

        while (running) {
            // Read Escape Button and eventually stop the program
            if (Button.ESCAPE.isDown()) {
                running = false;
                lcpThread.terminate();
            }
            if (LMDutils.interruptedSleep(100))
                break;
        }
    }

    /**
     * Initializes the right and left leg motors for stepping forward.
     */
    private static void initForward() {
        TouchSensor touchRight = new TouchSensor(SensorPort.S1);
        TouchSensor touchLeft = new TouchSensor(SensorPort.S2);

        // init right leg
        Motor.B.setSpeed(180);
        Motor.B.forward();
        do {
            Thread.yield();
        } while (touchRight.isPressed() == false);
        Motor.B.stop();

        // init left leg
        Motor.C.setSpeed(180);
        Motor.C.forward();
        do {
            Thread.yield();
        } while (touchLeft.isPressed() == false);
        Motor.C.rotate(180);

        initState = INIT_FORWARD;
    }

    /**
     * Moves the motors on the left and right for two steps
     * forward.
     * @param power how fast to move
     */
    private static void walkForward(int power) {
        if (initState != INIT_FORWARD)
            initForward();
        Motor.B.setSpeed(power*3);
        Motor.C.setSpeed(power*3);
        Motor.B.forward();
        Motor.C.forward();

        Motor.B.rotate(360, true);
        Motor.C.rotate(360);
    }

    /**
     * Initializes the right and left leg motors for stepping backward.
     */
    private static void initBackward() {
        TouchSensor touchRight = new TouchSensor(SensorPort.S1);
        TouchSensor touchLeft = new TouchSensor(SensorPort.S2);

        // init right leg
        Motor.B.setSpeed(180);
        Motor.B.backward();
        do {

            Thread.yield();
        } while (touchRight.isPressed() == false);
        Motor.B.stop();

        // init left leg
        Motor.C.setSpeed(180);
        Motor.C.backward();
        do {
            Thread.yield();
        } while (touchLeft.isPressed() == false);
        Motor.C.rotate(-180);

        initState = INIT_BACKWARD;
    }

    /**
     * Moves the motors on the left and right for two steps
     * backward.
     * @param power how fast to move
     */
    private static void walkBackward(int power) {
        if (initState != INIT_BACKWARD)
            initBackward();
        Motor.B.setSpeed(power*3);
        Motor.C.setSpeed(power*3);
        Motor.B.backward();
        Motor.C.backward();

        Motor.B.rotate(-360, true);
        Motor.C.rotate(-360);
    }

    /**
     * Reads the value of the Ultrasonice-Sensor and sends it as
     * TTS to the mobile phone when it is smaller than 30 cm.
     */
    private void commandUSSReader() {
        UltrasonicSensor myUSSensor = new UltrasonicSensor(SensorPort.S3);
        while (true) {
            if (myUSSensor.getDistance() < 30) {
                // Sound.playTone(1000, 500);
                if (lcpThread.sendTTS("Hello!", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("The distance is", 0, 0, 0, 2000))
                    break;
                if (lcpThread.sendTTS(""+myUSSensor.getDistance()+" centimeters", 0, 0, 0, 2000))
                    break;
            }
            if (LMDutils.interruptedSleep(200))
                break;
        }
    }

    /**
     * Reads the value of the Color-Light-Sensor and sends the name
     * as TTS to the mobile phone.
     */
    private void commandColorGuesser() {
        ColorSensor myColorSensor = new ColorSensor(SensorPort.S4, ColorSensor.TYPE_COLORFULL);
        while (true) {
            int myColor = myColorSensor.getColorID();
            if (myColor == ColorSensor.Color.GREEN) {
                if (lcpThread.sendTTS("green", 0, 0, 0, 1000))
                    break;
            } else if (myColor == ColorSensor.Color.RED) {
                // additional haptic feedback
                lcpThread.vibratePhone(1000);
                if (lcpThread.sendTTS("red", 0, 0, 0, 1000))
                    break;
            } else if (myColor == ColorSensor.Color.BLUE) {
                if (lcpThread.sendTTS("blue", 0, 0, 0, 1000))
                    break;
            } else if (myColor == ColorSensor.Color.YELLOW) {
                if (lcpThread.sendTTS("yellow", 0, 0, 0, 1000))
                    break;
            } else if (myColor == ColorSensor.Color.WHITE) {
                if (lcpThread.sendTTS("white", 0, 0, 0, 1000))
                    break;
            }

            if (LMDutils.interruptedSleep(300))
                break;
        }
        //myColorSensor.setType(ColorSensor.TYPE_COLORNONE);
    }

    /**
     * Performs a special command, defined via constants and
     * also delivers the needed parameters from LCP
     * @param commandNr the index of the command
     * @param parameter the LCP message array
     */
    public void performCommand(int command, byte[] parameter) {
        switch (command) {

            case HELLO_ROBOT:
                if (lcpThread.sendTTS("Hello, robot!", 0, 1, 2, 2000))
                    break;
                lcpThread.sendTTS("How are you?", 0, 1, 2, 3000);
                break;

            case HALLO_ROBOTER:
                if (lcpThread.sendTTS("Hallo, Roboter!", 1, 0, 0, 2000))
                    break;
                lcpThread.sendTTS("Wie geht es dir?", 1, 0, 0, 3000);
                break;

            case MUSIC_COMMAND:
                Compositions.play(Compositions.BACH_MUSETTE_D_MAJOR);
                break;

            case USS_READER:
                commandUSSReader();
                break;

            case COLOR_GUESSER:
                if (colorSensorInUse == false) {
                    colorSensorInUse = true;
                    commandColorGuesser();
                    colorSensorInUse = false;
                }
                break;

            case MINDdroidConnector.OUTPUT_COMMAND:
                // Just change variables for moving, so the other threads can decide
                // what to do and the bluetooth communication isn't blocked.
                switch (parameter[2]) {
                    case 1:
                        averageWalkingPower = parameter[3];
                        averageShakingPower = parameter[3];
                        break;
                    case 2:
                        averageWalkingPower += parameter[3];
                        averageWalkingPower /= 2;
                        if (Math.abs(averageWalkingPower) > 30)
                            walkingPower = averageWalkingPower;
                        else
                            walkingPower = 0;
                        averageShakingPower -= parameter[3];
                        averageShakingPower /= 2;
                        if (Math.abs(averageShakingPower) > 10)
                            shakingPower = averageShakingPower;
                        else
                            shakingPower = 0;
                        break;
                }
                break;

            case MINDdroidConnector.ACTION_COMMAND:
                boolean switchedOn = false;
                ColorSensor myColorSensor = null;
                if (colorSensorInUse == false) {
                    colorSensorInUse = true;
                    switchedOn = true;
                    myColorSensor = new ColorSensor(SensorPort.S4, ColorSensor.TYPE_COLORRED);
                    myColorSensor.setFloodlight(true);
                }
                Compositions.play(Compositions.BEETHOVEN_SYMPHONY_5_C_MINOR);
                if (switchedOn) {
                    myColorSensor.setFloodlight(false);
                    colorSensorInUse = false;
                }
                break;

            case MINDdroidConnector.DAEMON_1:
                // Display some nice effects
                LCD.setAutoRefresh(false);
                LCD.clear();
                int line = 7;
                while (true) {
                    LCD.drawString("MINDdroid", 3, line, false);
                    if (lcpThread.isConnected())
                        LCD.drawString("connected", 3, line+1, false);
                    LCD.refresh();
                    if (LMDutils.interruptedSleep(750))
                        break;
                    LCD.clear();
                    line-=1;
                    if (line < 0)
                        line = 7;
                }
                break;

            case MINDdroidConnector.DAEMON_2:
                // Shake the robot
                while (true) {
                    if (shakingPower == 0)
                        Motor.A.stop();
                    else {
                        Motor.A.setSpeed(180);
                        if (shakingPower > 0)
                            Motor.A.forward();
                        else
                            Motor.A.backward();
                    }
                    if (LMDutils.interruptedSleep(250))
                        break;
                }
                break;


            case MINDdroidConnector.DAEMON_3:
                // Walking of the robot
                while (true) {
                    if (walkingPower != 0) {
                        if (walkingPower > 0)
                            walkForward(walkingPower);
                        else
                            walkBackward(walkingPower);
                    }
                    if (LMDutils.interruptedSleep(100))
                        break;
                }
                break;

            default:
                break;

        }
    }

}


