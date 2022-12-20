package cgeo.geocaching.connector;

import java.util.Set;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaymarkingConnectorTest extends TestCase {

    private static IConnector getWaymarkingConnector() {
        final IConnector wmConnector = ConnectorFactory.getConnector("WM1234");
        assertThat(wmConnector).isNotNull();
        return wmConnector;
    }

    public static void testGetGeocodeFromUrl() {
        assertThat(ConnectorFactory.getGeocodeFromURL("http://coord.info/WM1234")).isEqualTo("WM1234");
        assertThat(ConnectorFactory.getGeocodeFromURL("http://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon")).isEqualTo("WMNCDT");

        final IConnector wmConnector = getWaymarkingConnector();

        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/WM1234")).isEqualTo("WM1234");
        assertThat(wmConnector.getGeocodeFromUrl("http://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon")).isEqualTo("WMNCDT");

        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/GC12ABC")).isNull();
        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/TB1234")).isNull();
    }

    public static void testCanHandle() {
        final IConnector wmConnector = getWaymarkingConnector();

        assertThat(wmConnector.canHandle("WM1234")).isTrue();
        assertThat(wmConnector.canHandle("GC1234")).isFalse();
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(getWaymarkingConnector().handledGeocodes(geocodes)).containsOnly("WM1234", "WM5678");
    }
}
