package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.connector.IConnector;

import java.util.Set;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
        assertThat(ocConnector).isNotNull();
        assertThat(ocConnector.getHost()).contains(".de");
        return ocConnector;
    }

    public static void testGetGeocodeFromUrlDe() throws Exception {
        final IConnector connector = ConnectorFactory.getConnector("OC0028");
        assertThat(connector.getGeocodeFromUrl("http://opencaching.de/OC0028")).isEqualTo("OC0028");
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/OC0028")).isEqualTo("OC0028");
    }

    public static void testGetGeocodeFromTextDe() throws Exception {
        final IConnector connector = ConnectorFactory.getConnector("OC0028");
        assertThat(connector.getGeocodeFromText("Bla http://www.opencaching.de/OC0028 Test")).isEqualTo("OC0028");
    }

    public static void testGetGeocodeFromTextUk() throws Exception {
        final IConnector connector = ConnectorFactory.getConnector("OK04F8");
        assertThat(connector.getGeocodeFromText("Bla https://opencache.uk/viewcache.php?wp=OK04F8 Test")).isEqualTo("OK04F8");
    }

    public static void testGetGeocodeFromInternalId() {
        final IConnector connector = ConnectorFactory.getConnector("OC0028");
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/viewcache.php?cacheid=151223")).isEqualTo("OCBBFE");
    }

    public static void testGetGeocodeFromUrlUs() throws Exception {
        final IConnector connector = ConnectorFactory.getConnector("OU07A0");
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.us/viewcache.php?wp=OU07A0")).isEqualTo("OU07A0");
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getOcDeConnector().handledGeocodes(geocodes)).containsOnly("OC1234", "OC5678");
    }
}
