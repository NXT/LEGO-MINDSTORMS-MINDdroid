package at.htlklu.nxtcontrol;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class at.htlklu.nxtcontrol.NXTControlTest \
 * at.htlklu.nxtcontrol.tests/android.test.InstrumentationTestRunner
 */
public class NXTControlTest extends ActivityInstrumentationTestCase<NXTControl> {

    public NXTControlTest() {
        super("at.htlklu.nxtcontrol", NXTControl.class);
    }

}
