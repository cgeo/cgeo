package cgeo.geocaching.connector.trackable;

import junit.framework.TestCase;

public class GeokretyConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertTrue(new GeokretyConnector().canHandleTrackable("GK82A2"));
        assertFalse(new GeokretyConnector().canHandleTrackable("GKXYZ1")); // non hex
        assertFalse(new GeokretyConnector().canHandleTrackable("TB1234"));
        assertFalse(new GeokretyConnector().canHandleTrackable("UNKNOWN"));
    }

}
