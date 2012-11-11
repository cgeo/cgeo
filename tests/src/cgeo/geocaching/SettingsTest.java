package cgeo.geocaching;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

@TargetApi(8)
public class SettingsTest extends ActivityInstrumentationTestCase2<cgeo> {

    public SettingsTest() {
        super(cgeo.class);
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
        final String mapFile = Settings.getMapFile();
        assertNotNull(mapFile);
        // We just want to ensure that it does not throw any exception but we do not know anything about the result
        MapsforgeMapProvider.isValidMapFile(mapFile);
    }

    public static void testSettings() {
        assertEquals(Settings.getMemberStatus(), GCConstants.MEMBER_STATUS_PM);
    }
}
