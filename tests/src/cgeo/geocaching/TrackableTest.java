package cgeo.geocaching;

import android.test.AndroidTestCase;

public class TrackableTest extends AndroidTestCase {

    public static void testGetGeocode() {
        cgTrackable trackable = new cgTrackable();
        trackable.setGeocode("tb1234");
        assertEquals("TB1234", trackable.getGeocode());
    }

    public static void testSetLogsNull() {
        cgTrackable trackable = new cgTrackable();
        trackable.setLogs(null);
        assertNotNull("Trackable logs must not be null!", trackable.getLogs());
    }
}
