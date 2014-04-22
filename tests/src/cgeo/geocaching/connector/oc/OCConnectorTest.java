package cgeo.geocaching.connector.oc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;

import junit.framework.TestCase;

public class OCConnectorTest extends TestCase {

    /**
     * OC.DE used up the 4 digit/character name space and switched over to 5 recently
     */
    public static void testCanHandleNew5DigitCodes() {
        final IConnector ocConnector = getOcDeConnector();
        assertThat(ocConnector.canHandle("OCFFFF")).isTrue();
        assertThat(ocConnector.canHandle("OC10000")).isTrue();
    }

    private static IConnector getOcDeConnector() {
        final IConnector ocConnector = ConnectorFactory.getConnector("OCXXX");
        assertThat(ocConnector.getHost().contains(".de")).isTrue();
        return ocConnector;
    }

}
