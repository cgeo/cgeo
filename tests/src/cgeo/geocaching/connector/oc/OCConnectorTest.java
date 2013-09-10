package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;

import junit.framework.TestCase;

public class OCConnectorTest extends TestCase {

    /**
     * OC.DE used up the 4 digit/character name space and switched over to 5 recently
     */
    public static void testCanHandleNew5DigitCodes() {
        final IConnector ocConnector = getOcDeConnector();
        assertTrue(ocConnector.canHandle("OCFFFF"));
        assertTrue(ocConnector.canHandle("OC10000"));
    }

    private static IConnector getOcDeConnector() {
        final IConnector ocConnector = ConnectorFactory.getConnector("OCXXX");
        assertTrue(ocConnector.getHost().contains(".de"));
        return ocConnector;
    }

}
