package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class TravelBugConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertTrue(getConnector().canHandleTrackable("TB1234"));
        assertTrue(getConnector().canHandleTrackable("TB1"));
        assertTrue(getConnector().canHandleTrackable("TB123F"));
        assertTrue(getConnector().canHandleTrackable("TB123Z"));
        assertTrue(getConnector().canHandleTrackable("TB4JD36")); // existing TB, 5 specific characters
        assertTrue(getConnector().canHandleTrackable("GK1234")); // valid secret code, even though this might be a geokrety
        assertTrue(getConnector().canHandleTrackable("GST9HV")); // existing secret code
        assertFalse(getConnector().canHandleTrackable("UNKNOWN"));
    }

    public static void testGetUrl() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("TB2345");
        assertEquals("http://www.geocaching.com//track/details.aspx?tracker=TB2345", getConnector().getUrl(trackable));
    }

    public static void testOnlineSearchBySecretCode() {
        Trackable trackable = getConnector().searchTrackable("GST9HV", null, null);
        assertNotNull(trackable);
        assertEquals("Deutschland", trackable.getName());
    }

    public static void testOnlineSearchByPublicCode() {
        Trackable trackable = getConnector().searchTrackable("TB4JD36", null, null);
        assertNotNull(trackable);
        assertEquals("Mein Kilometerzähler", trackable.getName());
    }

    private static TravelBugConnector getConnector() {
        return TravelBugConnector.getInstance();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertEquals("TB1234", TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/TB1234"));
        assertEquals("TB1234", TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/TB1234"));
        assertEquals("TB1234", TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://geocaching.com/track/details.aspx?tracker=TB1234"));
        assertEquals("TB1234", TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.geocaching.com/track/details.aspx?tracker=TB1234"));

        // do not match coord.info URLs of caches
        assertNull(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/GC1234"));
        assertNull(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/GC1234"));
    }
}
