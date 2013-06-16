package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class TravelBugConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertTrue(new TravelBugConnector().canHandleTrackable("TB1234"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB1"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB123F"));
        assertTrue(new TravelBugConnector().canHandleTrackable("TB123Z"));
        assertFalse(new TravelBugConnector().canHandleTrackable("GK1234"));
        assertFalse(new TravelBugConnector().canHandleTrackable("UNKNOWN"));
    }

    public static void testGetUrl() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("TB2345");
        assertEquals("http://www.geocaching.com//track/details.aspx?tracker=TB2345", new TravelBugConnector().getUrl(trackable));
    }

}
