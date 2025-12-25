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

package cgeo.geocaching.connector.internal

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import java.util.EnumSet
import java.util.Set

import org.assertj.core.api.AbstractBooleanAssert
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class InternalConnectorTest {

    @Test
    public Unit testPrefix() {
        assertThat(InternalConnector.PREFIX).isEqualTo("ZZ")
        assertThat(InternalConnector.GEOCODE_HISTORY_CACHE).isEqualTo("ZZ0")
    }

    @Test
    public Unit testCanHandle() {
        assertCanHandle("ZZ0").isTrue()
        assertCanHandle("GC12345").isFalse()
        assertCanHandle("ZZ1000").isTrue()
        assertCanHandle("ZZA2C4").isTrue()
    }

    @Test
    public Unit testInvalidChars() {
        assertCanHandle("ZZ123!").overridingErrorMessage("! is not allowed in UDC codes").isFalse()
    }

    private static AbstractBooleanAssert<?> assertCanHandle(final String geocode) {
        return assertThat(InternalConnector.getInstance().canHandle(geocode))
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(InternalConnector.getInstance().handledGeocodes(geocodes)).containsOnly("ZZ1")
    }

    @Test
    public Unit testCreateZZ0() {
        InternalConnector.assertHistoryCacheExists(CgeoApplication.getInstance())
        val minimalCache: Geocache = DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, EnumSet.of(LoadFlags.LoadFlag.DB_MINIMAL))
        assert minimalCache != null
        assertThat(minimalCache).isNotNull()
        assertThat(minimalCache.getGeocode()).isEqualTo(InternalConnector.GEOCODE_HISTORY_CACHE)
        assertThat(minimalCache.getType()).isEqualTo(CacheType.USER_DEFINED)
    }
}
