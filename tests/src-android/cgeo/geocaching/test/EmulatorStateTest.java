package cgeo.geocaching.test;

import cgeo.geocaching.utils.EnvironmentUtils;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class EmulatorStateTest extends TestCase {

    public static void testWritableMedia() {
        // check the emulator running our tests
        assertThat(EnvironmentUtils.isExternalStorageAvailable()).isTrue();
    }
}
