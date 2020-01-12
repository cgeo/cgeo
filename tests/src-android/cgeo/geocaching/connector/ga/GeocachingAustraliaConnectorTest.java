package cgeo.geocaching.connector.ga;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import java.util.Set;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocachingAustraliaConnectorTest extends TestCase {

    private static IConnector getGeocachingAustraliaConnector() {
        final IConnector gaConnector = ConnectorFactory.getConnector("GA1234");
        assertThat(gaConnector).isNotNull();
        return gaConnector;
    }

    public static void testCanHandle() {
        final IConnector wmConnector = getGeocachingAustraliaConnector();

        assertThat(wmConnector.canHandle("GA1234")).isTrue();
        assertThat(wmConnector.canHandle("GAAB12")).isFalse();
        assertThat(wmConnector.canHandle("TP1234")).isTrue();
        assertThat(wmConnector.canHandle("TPAB12")).isFalse();
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getGeocachingAustraliaConnector().handledGeocodes(geocodes)).containsOnly("GA1234", "TP1234", "GA5678", "TP5678");
    }
}
