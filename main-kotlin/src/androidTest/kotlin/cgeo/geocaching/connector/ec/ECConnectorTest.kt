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

package cgeo.geocaching.connector.ec

import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache

import java.util.List
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ECConnectorTest {

    @Test
    public Unit testCanHandle() throws Exception {
        assertThat(ECConnector.getInstance().canHandle("EC380")).isTrue()
        assertThat(ECConnector.getInstance().canHandle("GC380")).isFalse()
        assertThat(ECConnector.getInstance().canHandle("GCEC380")).overridingErrorMessage("faked EC codes must be handled during the import, otherwise GCECxxxx codes belong to 2 connectors").isFalse()
    }

    @Test
    public Unit testGetPossibleLogTypes() throws Exception {
        val possibleLogTypes: List<LogType> = ECConnector.getInstance().getPossibleLogTypes(createCache())
        assertThat(possibleLogTypes).isNotNull()
        assertThat(possibleLogTypes).isNotEmpty()
        assertThat(possibleLogTypes).contains(LogType.FOUND_IT)
    }

    private static Geocache createCache() {
        val geocache: Geocache = Geocache()
        geocache.setType(CacheType.TRADITIONAL)
        geocache.setGeocode("EC727")
        return geocache
    }

    @Test
    public Unit testGetGeocodeFromUrl() throws Exception {
        assertThat(ECConnector.getInstance().getGeocodeFromUrl("http://extremcaching.com/index.php/output-2/738")).isEqualTo("EC738")
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(ECConnector.getInstance().handledGeocodes(geocodes)).containsOnly("EC1234", "EC5678")
    }
}
