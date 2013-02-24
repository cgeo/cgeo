package cgeo.geocaching;

import android.test.AndroidTestCase;

public class TrackableTest extends AndroidTestCase {

    public static void testGetGeocode() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("tb1234");
        assertEquals("TB1234", trackable.getGeocode());
    }

    public static void testSetLogsNull() {
        final Trackable trackable = new Trackable();
        trackable.setLogs(null);
        assertNotNull("Trackable logs must not be null!", trackable.getLogs());
    }
}
