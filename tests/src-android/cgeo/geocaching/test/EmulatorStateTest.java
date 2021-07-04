package cgeo.geocaching.test;

import cgeo.geocaching.utils.EnvironmentUtils;

import android.os.Environment;

import junit.framework.TestCase;

public class EmulatorStateTest extends TestCase {

    public static void testWritableMedia() {
        // check the emulator running our tests
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            //fail test, but provide some information with it
            fail("external storage state not as expected, is: '" + Environment.getExternalStorageState() + "'");
        }
    }
}
