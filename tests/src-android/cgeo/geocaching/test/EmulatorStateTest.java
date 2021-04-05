package cgeo.geocaching.test;


import cgeo.geocaching.utils.EnvironmentUtils;

import android.test.suitebuilder.annotation.Suppress;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class EmulatorStateTest extends TestCase {

    @Suppress
    public static void testWritableMedia() {
        // check the emulator running our tests
        assertThat(EnvironmentUtils.isExternalStorageAvailable()).isTrue();
    }
}
