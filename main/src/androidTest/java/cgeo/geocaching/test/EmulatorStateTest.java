package cgeo.geocaching.test;

import cgeo.geocaching.utils.EnvironmentUtils;

import android.os.Environment;

import org.junit.Test;
import static org.junit.Assert.fail;

public class EmulatorStateTest {

    @Test
    public void testWritableMedia() {
        // check the emulator running our tests
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            //fail test, but provide some information with it
            fail("external storage state not as expected, is: '" + Environment.getExternalStorageState() + "'");
        }
    }
}
