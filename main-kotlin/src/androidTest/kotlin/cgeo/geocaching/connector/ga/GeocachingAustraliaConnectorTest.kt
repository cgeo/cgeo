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

package cgeo.geocaching.connector.ga

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.connector.IConnector

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeocachingAustraliaConnectorTest {

    private static IConnector getGeocachingAustraliaConnector() {
        val gaConnector: IConnector = ConnectorFactory.getConnector("GA1234")
        assertThat(gaConnector).isNotNull()
        return gaConnector
    }

    @Test
    public Unit testCanHandle() {
        val wmConnector: IConnector = getGeocachingAustraliaConnector()

        assertThat(wmConnector.canHandle("GA1234")).isTrue()
        assertThat(wmConnector.canHandle("GAAB12")).isFalse()
        assertThat(wmConnector.canHandle("TP1234")).isTrue()
        assertThat(wmConnector.canHandle("TPAB12")).isFalse()
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(getGeocachingAustraliaConnector().handledGeocodes(geocodes)).containsOnly("GA1234", "TP1234", "GA5678", "TP5678")
    }
}
