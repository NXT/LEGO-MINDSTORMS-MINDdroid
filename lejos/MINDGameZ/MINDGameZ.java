/**
 *   Copyright 2011 Guenther Hoelzl
 *
 *   This file is part of lejosMINDdroid.
 *
 *   lejosMINDdroid is free software: you can redistribute it and/or modifyA
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
 * This class is for implementing the first real game combining MINDdroid
 * and NXT robots.
 * The motors/sensors have to be connected as the following:
 * Motor left: PORT C
 * Motor right: PORT B
 * Light/Color Sensor: PORT 3
 */
public class MINDGameZ implements CommandPerformer {
    // implemented commands
    private final static int SELECT_LIGHT_SENSOR = 0;
    private final static int SELECT_COLOR_SENSOR = 1;
    private final static int CALIBRATE_SENSOR = 2;
    private final static int START_GAME = 3;
    private final static int STOP_GAME = 4;

    // Init states for moving of the robot
    private final static int NOT_INIT = 0;

    // Minimum/Maximum
    private final static int MAX_PLAYERS = 9;
    private final static int MAX_ROUNDS = 9;
    private final static int MIN_SPEED = 100;
    private final static int MAX_SPEED = 300;

    // THRESHOLD of light/color sensor
    private final static int LIGHT_THRESHOLD = 500;

    // States of the game
    private final static int NOT_STARTED = 0;
    private final static int GAME_STARTED = 1;
    private final static int ENTERING_NR_PLAYERS = 2;
    private final static int ENTERING_NR_ROUNDS = 3;
    private final static int READY_TO_PLAY = 4;
    private final static int ANNOUNCE_PLAYER = 5;
    private final static int WAITING_FOR_ACTION = 6;
    private final static int PLAYING = 7;
    private final static int CALIBRATING = 8;

    private static int initState = NOT_INIT;
    private static int leftMotorPower;
    private static int rightMotorPower;
    private static int steering = 0;
    private static int gameState = NOT_STARTED;
    private static int nrPlayers = 2;
    private static int currentPlayer;
    private static int nrRounds = 3;
    private static int currentRound;   
    private static int currentNumber;
    private static int currentThreshold = LIGHT_THRESHOLD;

    // we support both sensor types
    private static boolean colorSensorConnected = true;
    private static LightSensor lightSensor = null;
    private static int lightValue;
    private static ColorSensor colorSensor = null;

    private static int speed;
    private static byte speedPrescaler;
    private static int[] score;
    private static MINDdroidConnector lcpThread;    

    /**
     * The start of the program. Does registering of the commands.
     * @args The command line paramaters
     */
    public static void main(String[] args ) throws Exception {
        lcpThread = new MINDdroidConnector(new MINDGameZ());
        lcpThread.registerCommand("SELECT LIGHT SENSOR", SELECT_LIGHT_SENSOR);
        lcpThread.registerCommand("SELECT COLOR SENSOR", SELECT_COLOR_SENSOR);
        lcpThread.registerCommand("CALIBRATE SENSOR", CALIBRATE_SENSOR);
        lcpThread.registerCommand("START NEW GAME", START_GAME);
        lcpThread.registerCommand("STOP GAME", STOP_GAME);

        // also register the output and action command here,
        // so we can decide for ourselves what to do when
        // we get new motor commands or action button press
        // from MINDdroid
        lcpThread.registerCommand("OUTPUT", MINDdroidConnector.OUTPUT_COMMAND);
        lcpThread.registerCommand("ACTION", MINDdroidConnector.ACTION_COMMAND);
        // register the command for displaying effects and start it as a thread
        lcpThread.registerCommand("DISPLAY", MINDdroidConnector.DAEMON_1);
        lcpThread.startThreadForCommand(MINDdroidConnector.DAEMON_1);
        // register the command for driving the robot and start it as a thread
        lcpThread.registerCommand("DRIVE", MINDdroidConnector.DAEMON_2);
        lcpThread.startThreadForCommand(MINDdroidConnector.DAEMON_2);

        lcpThread.setDaemon(true);
        lcpThread.start();

        score = new int[MAX_PLAYERS];

        stopMotors();

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
     * Inititializes the sensor and the motors of the NXT
     */
    private static void stopMotors() {
        speed = MIN_SPEED;
        speedPrescaler = 0;
        Motor.B.stop();
        Motor.C.stop();
    }


    /**
     * Initialize the color/ligh sensor and wait some time for stabilization
     * @return when the method was interrupted
     */
    private static boolean initSensor() {
        if ((colorSensor == null) && (lightSensor == null)) {
            if (colorSensorConnected) {
                colorSensor = new ColorSensor(SensorPort.S3, ColorSensor.TYPE_COLORRED);
                colorSensor.setFloodlight(true);
            }
            else
                lightSensor = new LightSensor(SensorPort.S3);
            if (LMDutils.interruptedSleep(200))
                return false;
        }
        return true;
    }

    /**
     * @return the current sensor value of the attached light/colorsensor
     */
    private static int getSensorValue() {
        if (colorSensorConnected) 
            return colorSensor.getRawLightValue();
        else
            return lightSensor.readNormalizedValue();
    }

    /**
     * Displays text on the LCD depending on the current
     * state of the game.
     * @parame line the display position of the first line
     */
    private void displayText(int line) {
        String displayText1 = null;
        String displayText2 = null;

        switch (gameState) {

            case GAME_STARTED:
                displayText1 = "NEW GAME";
                break;

            case ENTERING_NR_PLAYERS:
                displayText1 = "Players: "+currentNumber;
                break;

            case ENTERING_NR_ROUNDS:
                displayText1 = "Rounds: "+currentNumber;
                break;

            case ANNOUNCE_PLAYER:
            case WAITING_FOR_ACTION:
            case PLAYING:
                displayText1 = "Player "+(currentPlayer+1)+" Round "+(currentRound+1);
                displayText2 = "Score: "+score[currentPlayer];
                break;

            case CALIBRATING:
                displayText1 = "Calibrating";
                displayText2 = "Threshold: "+currentThreshold;
                break;

            default:
                displayText1 = "MINDGameZ";
                if (lcpThread.isConnected())
                    displayText2 = "connected";
                break;
        }
        
        if (displayText1 != null)
            LCD.drawString(displayText1, 8-displayText1.length()/2, line);
        if (displayText2 != null)
            LCD.drawString(displayText2, 8-displayText2.length()/2, line+1);
    }

    /**
     * Lets the user(s) enter the number of players or number of 
     * rounds on the NXT brick.
     * @return false if the method was interrupted
     */
    private boolean enterNumber(int maximum) {
        boolean leftReleased = true;
        boolean rightReleased = true;
        while (true) {
            if (Button.ENTER.isDown())
                return true;                
            if (Button.RIGHT.isDown()) {
                if (rightReleased) 
                    if (currentNumber < maximum)
                        currentNumber++;
                rightReleased = false;
            }
            else
                rightReleased = true;

            if (Button.LEFT.isDown()) {
                if (leftReleased)
                    if (currentNumber > 1)
                        currentNumber--;
                leftReleased = false;
            }
            else
                leftReleased = true;
            if (LMDutils.interruptedSleep(100))
                return false;
        }
    }

    /**
     * Waits for pressing the orange enter button 
     * @return false if the method was interrupted
     */
    private boolean waitForEnter() {
        while (true) {
            if (Button.ENTER.isDown())
                return true;                
            if (LMDutils.interruptedSleep(100))
                return false;            
        }
    }

    /**
     * Performs a special command, defined via constants and
     * also delivers the needed parameters from LCP
     * @param commandNr the index of the command
     * @param parameter the LCP message array
     */
    public void performCommand(int command, byte[] parameter) {
        switch (command) {

            case CALIBRATE_SENSOR:
                gameState = CALIBRATING;
                if (initSensor() == false) 
                    break;
                if (lcpThread.sendTTS("Calibrating sensor", 0, 0, 0, 3000))
                    break;
                if (lcpThread.sendTTS("Place the sensor", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("over a white area", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("and press", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("the orange button", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("to ENTER", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("on the NXT", 0, 0, 0, 1500))
                    break;
                if (waitForEnter() == false)
                    break;
                int whiteValue = getSensorValue();
                if (lcpThread.sendTTS("Place the sensor", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("over a black area", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("and press again", 0, 0, 0, 1500))
                    break;
                if (lcpThread.sendTTS("the orange button", 0, 0, 0, 1500))
                    break;
                if (waitForEnter() == false)
                    break;
                int blackValue = getSensorValue();
                currentThreshold = (blackValue + whiteValue)/2;
                if (lcpThread.sendTTS("Sensor calibrated", 0, 0, 0, 3000))
                    break;
                gameState = NOT_STARTED;
                break;

            case START_GAME:                
                gameState = GAME_STARTED;
                stopMotors();
                if (lcpThread.sendTTS("New game started!", 0, 0, 0, 3000))
                    break;
                // enter number of players
                gameState = ENTERING_NR_PLAYERS;
                currentNumber = nrPlayers;
                if (lcpThread.sendTTS("Enter number", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("of players", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("at the N X T", 0, 0, 0, 1000))
                    break;
                if (enterNumber(MAX_PLAYERS) == false)
                    break;
                nrPlayers = currentNumber;
                String speechString = Integer.toString(nrPlayers);
                if (nrPlayers == 1)
                    speechString += " player selected";
                else
                    speechString += " players selected";
                if (lcpThread.sendTTS(speechString, 0, 0, 0, 2000))
                    break;
                // reset scores of the players
                for (int player = 0; player < nrPlayers; player++)
                    score[player] = 0;
                // enter number of rounds
                gameState = ENTERING_NR_ROUNDS;
                currentNumber = nrRounds;
                if (lcpThread.sendTTS("Enter number", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("of rounds", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("at the N X T", 0, 0, 0, 1000))
                    break;
                if (enterNumber(MAX_ROUNDS) == false)
                    break;
                nrRounds = currentNumber;
                speechString = Integer.toString(nrRounds);
                if (nrRounds == 1)
                    speechString += " round selected";
                else
                    speechString += " rounds selected";
                if (lcpThread.sendTTS(speechString, 0, 0, 0, 2000))
                    break;
                gameState = READY_TO_PLAY;                
                break;

            case STOP_GAME:
                gameState = NOT_STARTED;
                stopMotors();
                if (lcpThread.sendTTS("Game ended", 0, 0, 0, 2000))
                    break;                
                // find best player
                int bestPlayer = 0;
                int maxScore = 0;
                for (int player = 0; player < nrPlayers; player++) {
                    if (score[player] > maxScore) {
                        bestPlayer = player;
                        maxScore = score[player];
                    }
                }
                if (maxScore == 0)
                    break;                
                if (lcpThread.sendTTS("Player " + (bestPlayer+1), 0, 0, 0, 2000))
                    break;                
                if (lcpThread.sendTTS("won with score", 0, 0, 0, 2000))
                    break;
                if (lcpThread.sendTTS(Integer.toString(maxScore), 0, 0, 0, 4000))
                    break;
                if (lcpThread.sendTTS("CONGRATULATIONS!", 0, 0, 0, 2000))
                    break;
                break;                                   

            case SELECT_LIGHT_SENSOR:
            case SELECT_COLOR_SENSOR:
                // only allowed to do this if not playing/driving
                if (gameState == PLAYING || gameState == CALIBRATING) {
                    lcpThread.sendTTS("not allowed", 0, 0, 0, 2000);
                    break;
                }
                colorSensor = null;
                lightSensor = null;                    
                colorSensorConnected = (command == SELECT_COLOR_SENSOR);
                if (lcpThread.sendTTS(colorSensorConnected ? "Color-Sensor" : "Light-Sensor", 0, 0, 0, 1000))
                    break;
                if (lcpThread.sendTTS("selected", 0, 0, 0, 1000))
                    break;
                break;      
                          
            case MINDdroidConnector.OUTPUT_COMMAND:
                // Just change variables for moving, so the other threads can decide
                // what to do and the bluetooth communication isn't blocked.
                switch (parameter[2]) {
                    case 1:
                        leftMotorPower = parameter[3];
                        break;
                    case 2:
                        rightMotorPower = parameter[3];
                        steering = (leftMotorPower - rightMotorPower) / 2;
                        break;
                }
                break;

            case MINDdroidConnector.ACTION_COMMAND:
                if (gameState == WAITING_FOR_ACTION) {
                    if (lcpThread.sendTTS("Good luck!", 0, 0, 0, 2000))
                        break;
                    gameState = PLAYING;
                }                    
                break;

            case MINDdroidConnector.DAEMON_1:
                // Display some nice effects
                LCD.setAutoRefresh(false);
                LCD.clear();
                int line = 7;  
                while (true) {
                    displayText(line--);
                    LCD.refresh();
                    if (LMDutils.interruptedSleep(750))
                        break;
                    LCD.clear();
                    if (line < 0)
                        line = 7;
                }
                break;

            case MINDdroidConnector.DAEMON_2:
                // the main game actions: announcing, waiting for actions, playing

                outerLoop:                
                while (true) {
                    
                    switch (gameState) {
                        // drive motors according to the steering
                        // wait for light sensor reading
                        // if light sensor reading indicates failure => STOP and change player
                        case PLAYING:
                            if (initSensor() == false) 
                                break outerLoop;
                            // read light sensor and eventually switch players
                            lightValue = getSensorValue();
                            if (lightValue < currentThreshold) {
                                // vibrate, stop the motors and switch players
                                lcpThread.vibratePhone(1000);
                                stopMotors();
                                if (lcpThread.sendTTS("STOP!", 0, 0, 0, 1000))
                                    break outerLoop;
                                if (lcpThread.sendTTS("Your score is", 0, 0, 0, 1000))
                                    break outerLoop;
                                if (lcpThread.sendTTS(Integer.toString(score[currentPlayer]), 0, 0, 0, 4000))
                                    break outerLoop;
                                if (++currentPlayer == nrPlayers) {
                                    currentPlayer = 0;
                                    // game finished ?
                                    if (++currentRound == nrRounds) {
                                        gameState = NOT_STARTED;
                                        lcpThread.startThreadForCommand(STOP_GAME);
                                        continue;
                                    }
                                }
                                gameState = ANNOUNCE_PLAYER;
                            }
                            else {
                                // set new motor speeds
                                Motor.B.forward();                                
                                Motor.B.setSpeed(speed + steering*speed/MIN_SPEED);
                                Motor.C.forward();
                                Motor.C.setSpeed(speed - steering*speed/MIN_SPEED);
                                // accelerate slowly
                                if (speed < MAX_SPEED) {
                                    if (++speedPrescaler == 3) {
                                        speedPrescaler = 0;
                                        speed++;
                                    }
                                }
                                // update score of player
                                // the higher the speed the more points 
                                score[currentPlayer] += speed/50;
                            }
                            break;
    
                        // init game variables
                        case READY_TO_PLAY:
                            currentPlayer = 0;
                            currentRound = 0;
                            gameState = ANNOUNCE_PLAYER;
                            break;

                        // announce and wait for the current player
                        case ANNOUNCE_PLAYER:
                            if (lcpThread.sendTTS("Player "+(currentPlayer+1), 0, 0, 0, 1500))
                                break outerLoop;                        
                            if (lcpThread.sendTTS("Reposition the", 0, 0, 0, 1500))
                                break outerLoop;                        
                            if (lcpThread.sendTTS("N X T, and", 0, 0, 0, 1500))
                                break outerLoop;                        
                            if (lcpThread.sendTTS("press ACTION!", 0, 0, 0, 1500))
                                break outerLoop;                        
                            gameState = WAITING_FOR_ACTION;
                            break;
                        
                        default:
                            break;
                    }
          
                    if (LMDutils.interruptedSleep(50))
                        break;
                }
                break;

            default:
                break;

        }
    }

}


