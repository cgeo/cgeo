package cgeo.geocaching.test;

import android.os.Environment;

import junit.framework.TestCase;

public class EmulatorStateTest extends TestCase {

    public static void testWritableMedia() {
        // check the emulator running our tests
        assertTrue(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
    }
}
