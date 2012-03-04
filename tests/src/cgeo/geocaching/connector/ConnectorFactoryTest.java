package cgeo.geocaching.connector;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.opencaching.OpenCachingConnector;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.mock.GC1ZXX2;

public class ConnectorFactoryTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetConnectors() {
        final IConnector[] connectors = ConnectorFactory.getConnectors();
        assertNotNull(connectors);
        assertTrue(connectors.length > 0); // unknown connector must exist
    }

    public static void testCanHandle() {
        assertFalse(ConnectorFactory.canHandle(""));
        assertTrue(ConnectorFactory.canHandle("GC12345"));
        assertTrue(ConnectorFactory.canHandle("some string")); // using unknown connector
        assertFalse(ConnectorFactory.canHandle("[/start with special char"));
    }

    public static void testGeocodeOpenCaching() {
        assertTrue(ConnectorFactory.getConnector("OZ12345") instanceof OpenCachingConnector); // opencaching CZ
        assertTrue(ConnectorFactory.getConnector("OC12345") instanceof OpenCachingConnector); // opencaching DE
        assertTrue(ConnectorFactory.getConnector("OU12345") instanceof OpenCachingConnector); // opencaching US
        assertTrue(ConnectorFactory.getConnector("OK12345") instanceof OpenCachingConnector); // opencaching UK
        assertTrue(ConnectorFactory.getConnector("OJ12345") instanceof OpenCachingConnector); // opencaching JP
        assertTrue(ConnectorFactory.getConnector("OS12345") instanceof OpenCachingConnector); // opencaching NO
        assertTrue(ConnectorFactory.getConnector("OB12345") instanceof OpenCachingConnector); // opencaching NL
        assertTrue(ConnectorFactory.getConnector("OP12345") instanceof OpenCachingConnector); // opencaching PL
    }

    public static void testGeocodeInvalidFormat() throws Exception {
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

}
