package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(8)
public class SettingsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public SettingsTest() {
        super(MainActivity.class);
    }

    /**
     * access settings.
     * this should work fine without an exception (once there was an exception because of the empty map file string)
     */
    public static void testSettingsException() {
        final String mapFile = Settings.getMapFile();
        // We just want to ensure that it does not throw any exception but we do not know anything about the result
        MapsforgeMapProvider.isValidMapFile(mapFile);
        assertTrue(true);
    }

    public static void testSettings() {
        assertEquals(GCConstants.MEMBER_STATUS_PM, Settings.getMemberStatus());
    }

    public static void testDeviceHasNormalLogin() {
        // if the unit tests were interrupted in a previous run, the device might still have the "temporary" login data from the last tests
        assertFalse("c:geo".equals(Settings.getUsername()));
    }
}
