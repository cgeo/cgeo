package cgeo.geocaching.connector;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.oc.OCConnector;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.mock.GC1ZXX2;

import java.util.Collection;

public class ConnectorFactoryTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetConnectors() {
        final Collection<IConnector> connectors = ConnectorFactory.getConnectors();
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty()); // unknown connector must exist
    }

    public static void testCanHandle() {
        assertFalse(ConnectorFactory.canHandle(""));
        assertTrue(ConnectorFactory.canHandle("GC12345"));
        assertTrue(ConnectorFactory.canHandle("some string")); // using unknown connector
        assertFalse(ConnectorFactory.canHandle("[/start with special char"));
    }

    public static void testGeocodeOpenCaching() {
        assertTrue(ConnectorFactory.getConnector("OZ12345") instanceof OCConnector); // opencaching CZ
        assertTrue(ConnectorFactory.getConnector("OC12345") instanceof OCConnector); // opencaching DE
        assertTrue(ConnectorFactory.getConnector("OU12345") instanceof OCConnector); // opencaching US
        assertTrue(ConnectorFactory.getConnector("OK12345") instanceof OCConnector); // opencaching UK
        assertTrue(ConnectorFactory.getConnector("OJ12345") instanceof OCConnector); // opencaching JP
        assertTrue(ConnectorFactory.getConnector("OS12345") instanceof OCConnector); // opencaching NO
        assertTrue(ConnectorFactory.getConnector("OB12345") instanceof OCConnector); // opencaching NL
        assertTrue(ConnectorFactory.getConnector("OP12345") instanceof OCConnector); // opencaching PL
    }

    public static void testGeocodeInvalidFormat() {
        // all codes are invalid
        assertTrue(ConnectorFactory.getConnector("GC") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OC") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OX") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("GC 1234") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OC 1234") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OX 1234") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("GC-1234") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OC-1234") instanceof UnknownConnector);
        assertTrue(ConnectorFactory.getConnector("OX-1234") instanceof UnknownConnector);
    }

    public static void testGetConnectorCgCache() {
        assertEquals(GCConnector.getInstance(), ConnectorFactory.getConnector(new GC1ZXX2()));
    }

    public static void testGetConnectorString() {
        final IConnector connector = ConnectorFactory.getConnector("GC12345");
        assertNotNull(connector);
        assertEquals(GCConnector.getInstance().getName(), connector.getName());
    }

    public static void testTrim() {
        assertTrue(ConnectorFactory.getConnector("   OZ12345   ") instanceof OCConnector); // opencaching CZ
        assertTrue(ConnectorFactory.getConnector("   OZ 12345   ") instanceof UnknownConnector);
    }

    public static void testGetGeocodeFromUrl() {
        assertEquals("GC34PLO", ConnectorFactory.getGeocodeFromURL("http://coord.info/GC34PLO"));
        assertEquals("GC34PLO", ConnectorFactory.getGeocodeFromURL("http://www.coord.info/GC34PLO"));
        assertEquals("OX1234", ConnectorFactory.getGeocodeFromURL("http://www.opencaching.com/#!geocache/OX1234"));

        assertEquals("GC12ABC", GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/GC12ABC"));
        assertEquals("GC12ABC", GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/GC12ABC"));
        assertEquals("GC12ABC", GCConnector.getInstance().getGeocodeFromUrl("http://www.geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau"));
        assertEquals("GC12ABC", GCConnector.getInstance().getGeocodeFromUrl("http://geocaching.com/geocache/GC12ABC_die-muhlen-im-schondratal-muhle-munchau"));

        // trackable URLs
        assertNull(GCConnector.getInstance().getGeocodeFromUrl("http://coord.info/TB1234"));
        assertNull(GCConnector.getInstance().getGeocodeFromUrl("http://www.coord.info/TB1234"));

        // make sure that a mixture of different connector and geocode is recognized as invalid
        assertNull(ConnectorFactory.getGeocodeFromURL("http://www.opencaching.com/#!geocache/" + "GC12345"));
    }

    public static void testGetTrackableFromURL() throws Exception {
        assertEquals("GK78FA", ConnectorFactory.getTrackableFromURL("http://www.geokrety.org/konkret.php?id=30970"));
        assertEquals("GK78FA", ConnectorFactory.getTrackableFromURL("http://geokrety.org/konkret.php?id=30970"));
        assertEquals("TB1234", ConnectorFactory.getTrackableFromURL("http://coord.info/TB1234"));
        assertEquals("TB1234", ConnectorFactory.getTrackableFromURL("http://www.coord.info/TB1234"));
        assertEquals("TB1234", ConnectorFactory.getTrackableFromURL("http://geocaching.com/track/details.aspx?tracker=TB1234"));
        assertEquals("TB1234", ConnectorFactory.getTrackableFromURL("http://www.geocaching.com/track/details.aspx?tracker=TB1234"));

        // cache URLs
        assertNull(ConnectorFactory.getTrackableFromURL("http://coord.info/GC1234"));
        assertNull(ConnectorFactory.getTrackableFromURL("http://www.coord.info/GC1234"));
    }
}
