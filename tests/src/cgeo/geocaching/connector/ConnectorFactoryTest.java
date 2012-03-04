package cgeo.geocaching.connector;

import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.mock.GC1ZXX2;

public class ConnectorFactoryTest extends AbstractResourceInstrumentationTestCase {

    public static void testGetConnectors() {
        IConnector[] connectors = ConnectorFactory.getConnectors();
        assertNotNull(connectors);
        assertTrue(connectors.length > 0); // unknown connector must exist
    }

    public static void testCanHandle() {
        assertFalse(ConnectorFactory.canHandle(""));
        assertTrue(ConnectorFactory.canHandle("GC12345"));
        assertTrue(ConnectorFactory.canHandle("some string")); // using unknown connector
        assertFalse(ConnectorFactory.canHandle("[/start with special char"));
    }

    public static void testGetConnectorCgCache() {
        assertEquals(GCConnector.getInstance(), ConnectorFactory.getConnector(new GC1ZXX2()));
    }

    public static void testGetConnectorString() {
        IConnector connector = ConnectorFactory.getConnector("GC12345");
        assertNotNull(connector);
        assertEquals(GCConnector.getInstance().getName(), connector.getName());
    }

}
