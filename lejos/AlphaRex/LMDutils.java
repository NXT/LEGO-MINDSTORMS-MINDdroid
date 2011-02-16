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

/**
 * This class provides simple utility tasks.
 */
public class LMDutils {
    /**
      * Waits the given amount in ms and returns in the case
      * of interruption.
      * @param time the time to sleep in ms
      * @return true, if the method is interrupted
      */
    public static boolean interruptedSleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            return true;
        }
        return false;
    }
}

