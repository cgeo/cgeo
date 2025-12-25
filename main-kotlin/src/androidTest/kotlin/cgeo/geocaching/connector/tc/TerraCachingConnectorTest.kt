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

package cgeo.geocaching.connector.tc

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.connector.IConnector

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class TerraCachingConnectorTest {

    private static IConnector getTerraCachingConnector() {
        val tcConnector: IConnector = ConnectorFactory.getConnector("TCABC")
        assertThat(tcConnector).isNotNull()
        return tcConnector
    }

    @Test
    public Unit testHandleTerraCaches() {
        val tcConnector: IConnector = getTerraCachingConnector()

        assertThat(tcConnector.canHandle("TCABC")).isTrue()
        assertThat(tcConnector.canHandle("TC2JP")).isTrue()
        assertThat(tcConnector.canHandle("TC9")).isTrue()

        assertThat(tcConnector.canHandle("GC1234")).isFalse()
    }

    @Test
    public Unit testHandleCyberCaches() {
        val tcConnector: IConnector = getTerraCachingConnector()

        assertThat(tcConnector.canHandle("CC6KVG")).isTrue()
        assertThat(tcConnector.canHandle("CC7TMQ")).isTrue()
        assertThat(tcConnector.canHandle("CC9")).isTrue()
    }

    @Test
    public Unit testHandleLocationLessCaches() {
        val tcConnector: IConnector = getTerraCachingConnector()

        assertThat(tcConnector.canHandle("LC5U28")).isTrue()
        assertThat(tcConnector.canHandle("LC9")).isTrue()
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(getTerraCachingConnector().handledGeocodes(geocodes)).containsOnly("TCABC", "TC2JP")
    }
}
