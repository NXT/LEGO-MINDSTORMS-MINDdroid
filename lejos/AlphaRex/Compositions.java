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

/**
 * This class plays compositions for one voice.
 * @author Guenther Hoelzl
 * @version 1.0
 */
public class Compositions {
    public static int BACH_MUSETTE_D_MAJOR = 0;
    public static int BEETHOVEN_SYMPHONY_5_C_MINOR = 1;

    private final static byte PAUSE=-1;
    private final static byte GM=0;
    private final static byte GISM=1;
    private final static byte A0=2;
    private final static byte B0=3;
    private final static byte H0=4;
    private final static byte C0=5;
    private final static byte CIS0=6;
    private final static byte D0=7;
    private final static byte DIS0=8;
    private final static byte E0=9;
    private final static byte F0=10;
    private final static byte FIS0=11;
    private final static byte G0=12;
    private final static byte GIS0=13;
    private final static byte A1=14;
    private final static byte B1=15;
    private final static byte H1=16;
    private final static byte C1=17;
    private final static byte CIS1=18;
    private final static byte D1=19;
    private final static byte DIS1=20;
    private final static byte E1=21;
    private final static byte F1=22;
    private final static byte FIS1=23;
    private final static byte G1=24;
    private final static byte GIS1=25;
    private final static byte A2=26;

    private final static int TONE[] = {
        196, 208, 220, 233, 247, 262, 277, 294, 311, 330,
        349, 369, 392, 415, 440, 466, 494, 523, 554, 587,
        622, 659, 698, 739, 784, 831, 880
    };

    // Bach's "Musette in D Major"
    // see http://www.youtube.com/watch?v=osedN65xd_I&feature=related
    private static final byte [] MELODY_0 = {
        A2, 4, G1, 1, FIS1, 1, E1, 1, D1, 1,
        A2, 4, G1, 1, FIS1, 1, E1, 1, D1, 1,
        FIS0, 1, G0, 1, A1, 2, G0, 2, FIS0, 2,
        E0, 2, A1, 2, FIS0, 2, D0, 2,
        A2, 4, G1, 1, FIS1, 1, E1, 1, D1, 1,
        A2, 4, G1, 1, FIS1, 1, E1, 1, D1, 1,
        FIS0, 1, G0, 1, A1, 2, G0, 2, FIS0, 2,
        E0, 2, A1, 2, D0, 4,
        CIS1, 1, D1, 1, E1, 2, CIS1, 1, D1, 1, E1, 2,
        A2, 2, E1, 2, E1, 4,
        A2, 2, E1, 2, A2, 2, E1, 2,
        D1, 1, CIS1, 1, H1, 1, A1, 1, H1, 2, E0, 2,
        E1, 2, DIS1, 2, E0, 2, D1, 4, CIS1, 2, A2, 2, GIS1, 2,
        E1, 2, DIS1, 2, E0, 2, D1, 4, CIS1, 2, A2, 2, GIS1, 2,
        E1, 1, DIS1, 1, CIS1, 1, DIS1, 1, E1, 1, DIS1, 1, CIS1, 1, DIS1, 1,
        E1, 2, GIS0, 2, A1, 2, D1, 2,
        CIS1, 1, D1, 1, E1, 2, A1, 2, D0, 2,
        CIS0, 1, D0, 1, E0, 2, A0, 4,
        PAUSE, 12
    };

    // Beethoven "Symphony no.5 in C Minor"
    private static final byte [] MELODY_1 = {
        G0, 2, G0, 2, G0, 2, DIS0, 8,
        PAUSE, 2, F0, 2, F0, 2, F0, 2, D0, 8,
        PAUSE, 2, G0, 2, G0, 2, G0, 2,
        DIS0, 2, GIS0, 2, GIS0, 2, GIS0, 2, G0, 2,
        DIS1, 2, DIS1, 2, DIS1, 2, C1, 8
    };

    /**
     * Starts to play the melody an stops when it's interrupted.
     * @param composition The composition to be played, see constants
     */
    public static void play(int composition) {
        byte[] melody;
        switch (composition) {
            case 1:
                melody = MELODY_1;
                break;
            default:
                melody = MELODY_0;
                break;
        }

        int actNote = 0;
        while (actNote < melody.length) {
            if (melody[actNote] >= 0)
                Sound.playTone(TONE[melody[actNote]], melody[actNote+1]*120-20);
            try {
                Thread.sleep(120*melody[actNote+1]);
            } catch (InterruptedException e) {
                break;
            }
            actNote += 2;
        }
    }
}

