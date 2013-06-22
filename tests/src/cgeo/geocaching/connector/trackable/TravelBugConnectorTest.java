package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class TravelBugConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertTrue(new TravelBugConnector().canHandleTrackable("TB1234"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB1"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB123F"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB123Z"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB4JD36")); // existing TB, 5 specific characters
        assertTrue(new TravelBugConnector().canHandleTrackable("GK1234")); // valid secret code, even though this might be a geokrety
        assertTrue(new TravelBugConnector().canHandleTrackable("GST9HV")); // existing secret code
        assertFalse(new TravelBugConnector().canHandleTrackable("UNKNOWN"));
    }

    public static void testGetUrl() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("TB2345");
        assertEquals("http://www.geocaching.com//track/details.aspx?tracker=TB2345", new TravelBugConnector().getUrl(trackable));
    }

    public static void testOnlineSearchBySecretCode() {
        Trackable trackable = new TravelBugConnector().searchTrackable("GST9HV", null, null);
        assertNotNull(trackable);
        assertEquals("Deutschland", trackable.getName());
    }

    public static void testOnlineSearchByPublicCode() {
        Trackable trackable = new TravelBugConnector().searchTrackable("TB4JD36", null, null);
        assertNotNull(trackable);
        assertEquals("Mein Kilometerzähler", trackable.getName());
    }
}
