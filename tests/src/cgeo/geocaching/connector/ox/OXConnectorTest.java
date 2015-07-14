package cgeo.geocaching.connector.ox;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import junit.framework.TestCase;

import java.util.Set;

public class OXConnectorTest extends TestCase {

    private static IConnector getOXConnector() {
        final IConnector oxConnector = ConnectorFactory.getConnector("OXZZZZZ");
        assertThat(oxConnector).isNotNull();
        return oxConnector;
    }

    public static void testCanHandle() {
        // http://www.opencaching.com/api_doc/concepts/oxcodes.html
        final IConnector oxConnector = getOXConnector();
        assertThat(oxConnector.canHandle("OXZZZZZ")).isTrue();
        assertThat(oxConnector.canHandle("OX1")).isTrue();
        assertThat(oxConnector.canHandle("GCABCDE")).isFalse();
        assertThat(oxConnector.canHandle("OX_")).isFalse();
    }

    public static void testGetGeocodeFromUrl() {
        final IConnector connector = getOXConnector();
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.com/de/#!geocache/OX1R421")).isEqualTo("OX1R421");
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.com/#!geocache/OX1R421")).isEqualTo("OX1R421");
    }

    public static void testHandledGeocodes() {
        Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getOXConnector().handledGeocodes(geocodes)).containsOnly("OX1234", "OX5678");
    }
}
