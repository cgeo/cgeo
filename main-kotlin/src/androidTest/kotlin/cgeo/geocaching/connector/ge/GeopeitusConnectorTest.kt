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

package cgeo.geocaching.connector.ge

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.connector.IConnector

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeopeitusConnectorTest {

    private static IConnector getGeopeitusConnector() {
        val geopeitusConnector: IConnector = ConnectorFactory.getConnector("GE1234")
        assertThat(geopeitusConnector).isNotNull()
        return geopeitusConnector
    }

    @Test
    public Unit testCanHandle() {
        val wmConnector: IConnector = getGeopeitusConnector()

        assertThat(wmConnector.canHandle("GE1234")).isTrue()
        assertThat(wmConnector.canHandle("GEAB12")).isFalse()
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(getGeopeitusConnector().handledGeocodes(geocodes)).containsOnly("GE1234", "GE5678")
    }
}
