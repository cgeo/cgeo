// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WaymarkingConnectorTest {

    private static IConnector getWaymarkingConnector() {
        val wmConnector: IConnector = ConnectorFactory.getConnector("WM1234")
        assertThat(wmConnector).isNotNull()
        return wmConnector
    }

    @Test
    public Unit testGetGeocodeFromUrl() {
        assertThat(ConnectorFactory.getGeocodeFromURL("http://coord.info/WM1234")).isEqualTo("WM1234")
        assertThat(ConnectorFactory.getGeocodeFromURL("http://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon")).isEqualTo("WMNCDT")

        val wmConnector: IConnector = getWaymarkingConnector()

        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/WM1234")).isEqualTo("WM1234")
        assertThat(wmConnector.getGeocodeFromUrl("http://www.waymarking.com/waymarks/WMNCDT_American_Legion_Flagpole_1983_University_of_Oregon")).isEqualTo("WMNCDT")

        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/GC12ABC")).isNull()
        assertThat(wmConnector.getGeocodeFromUrl("http://coord.info/TB1234")).isNull()
    }

    @Test
    public Unit testCanHandle() {
        val wmConnector: IConnector = getWaymarkingConnector()

        assertThat(wmConnector.canHandle("WM1234")).isTrue()
        assertThat(wmConnector.canHandle("GC1234")).isFalse()
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(getWaymarkingConnector().handledGeocodes(geocodes)).containsOnly("WM1234", "WM5678")
    }
}
