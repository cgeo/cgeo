package cgeo.geocaching.connector.trackable;

import junit.framework.TestCase;

public class GeokretyConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertTrue(new GeokretyConnector().canHandleTrackable("GK82A2"));
        assertFalse(new GeokretyConnector().canHandleTrackable("GKXYZ1")); // non hex
        assertFalse(new GeokretyConnector().canHandleTrackable("TB1234"));
        assertFalse(new GeokretyConnector().canHandleTrackable("UNKNOWN"));
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertEquals("GK78FA", new GeokretyConnector().getTrackableCodeFromUrl("http://www.geokrety.org/konkret.php?id=30970"));
        assertEquals("GK78FA", new GeokretyConnector().getTrackableCodeFromUrl("http://geokrety.org/konkret.php?id=30970"));
    }

    public static void testGeocode() throws Exception {
        assertEquals("GK97C1", GeokretyConnector.geocode(38849));
    }

    public static void testGetId() throws Exception {
        assertEquals(38849, GeokretyConnector.getId("GK97C1"));
    }

}
