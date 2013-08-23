package cgeo.geocaching;

import android.test.AndroidTestCase;

public class TrackableTest extends AndroidTestCase {

    public static void testGetGeocode() {
        final Trackable trackable = createTrackable("tb1234");
        assertEquals("TB1234", trackable.getGeocode());
    }

    public static void testSetLogsNull() {
        final Trackable trackable = new Trackable();
        trackable.setLogs(null);
        assertNotNull("Trackable logs must not be null!", trackable.getLogs());
    }

    public static void testTrackableUrl() {
        final Trackable trackable = createTrackable("TB1234");
        assertEquals("http://www.geocaching.com//track/details.aspx?tracker=TB1234", trackable.getUrl());
    }

    public static void testGeokretUrl() {
        Trackable geokret = createTrackable("GK82A2");
        assertEquals("http://geokrety.org/konkret.php?id=33442", geokret.getUrl());
    }

    public static void testLoggable() {
        assertTrue(createTrackable("TB1234").isLoggable());
        assertFalse(createTrackable("GK1234").isLoggable());
    }

    private static Trackable createTrackable(String geocode) {
        final Trackable trackable = new Trackable();
        trackable.setGeocode(geocode);
        return trackable;
    }

}
