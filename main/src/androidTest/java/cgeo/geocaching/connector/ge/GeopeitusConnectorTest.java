package cgeo.geocaching.connector.ge;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import java.util.Set;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeopeitusConnectorTest extends TestCase {

    private static IConnector getGeopeitusConnector() {
        final IConnector geopeitusConnector = ConnectorFactory.getConnector("GE1234");
        assertThat(geopeitusConnector).isNotNull();
        return geopeitusConnector;
    }

    public static void testCanHandle() {
        final IConnector wmConnector = getGeopeitusConnector();

        assertThat(wmConnector.canHandle("GE1234")).isTrue();
        assertThat(wmConnector.canHandle("GEAB12")).isFalse();
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getGeopeitusConnector().handledGeocodes(geocodes)).containsOnly("GE1234", "GE5678");
    }
}
