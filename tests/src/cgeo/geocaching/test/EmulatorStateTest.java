package cgeo.geocaching.test;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.utils.EnvironmentUtils;

import android.os.Environment;

import junit.framework.TestCase;

public class EmulatorStateTest extends TestCase {

    public static void testWritableMedia() {
        // check the emulator running our tests
        assertThat(EnvironmentUtils.getExternalStorageState()).isEqualTo(Environment.MEDIA_MOUNTED);
    }
}
