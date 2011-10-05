package cgeo.geocaching;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgeo;

import android.test.ActivityInstrumentationTestCase2;

public class SettingsTest extends ActivityInstrumentationTestCase2<cgeo> {

    public SettingsTest() {
        super("cgeo.geocaching", cgeo.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * access settings.
     * this should work fine without an exception (once there was an exception because of the empty map file string)
     */
    public static void testSettingsException() {
        // asserts A OR NOT A, because we don't know what the settings are on any device or emulator
        assertTrue(Settings.isValidMapFile() || !Settings.isValidMapFile());
    }
}
