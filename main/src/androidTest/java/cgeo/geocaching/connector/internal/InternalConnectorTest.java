package cgeo.geocaching.connector.internal;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.connector.ConnectorFactoryTest;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;

import java.util.EnumSet;
import java.util.Set;

import org.assertj.core.api.AbstractBooleanAssert;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class InternalConnectorTest extends AbstractResourceInstrumentationTestCase {

    public static void testPrefix() {
        assertThat(InternalConnector.PREFIX).isEqualTo("ZZ");
        assertThat(InternalConnector.GEOCODE_HISTORY_CACHE).isEqualTo("ZZ0");
        assertThat(InternalConnector.geocodeFromId(7)).isEqualTo("ZZ7");
    }

    public static void testCanHandle() {
        assertCanHandle("ZZ0").isTrue();
        assertCanHandle("GC12345").isFalse();
        assertCanHandle("ZZ1000").isTrue();
    }

    public static void testInvalidChars() {
        assertCanHandle("ZZ123!").overridingErrorMessage("! is not allowed in UDC codes").isFalse();
    }

    private static AbstractBooleanAssert<?> assertCanHandle(final String geocode) {
        return assertThat(InternalConnector.getInstance().canHandle(geocode));
    }

    public static void testHandledGeocodes() {
        final Set<String> geocodes = ConnectorFactoryTest.getGeocodeSample();
        assertThat(InternalConnector.getInstance().handledGeocodes(geocodes)).containsOnly("ZZ1");
    }

    public void testCreateZZ0() {
        InternalConnector.assertHistoryCacheExists(CgeoApplication.getInstance());
        final Geocache minimalCache = DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, EnumSet.of(LoadFlags.LoadFlag.DB_MINIMAL));
        assert minimalCache != null;
        assertThat(minimalCache).isNotNull();
        assertThat(minimalCache.getGeocode()).isEqualTo(InternalConnector.GEOCODE_HISTORY_CACHE);
        assertThat(minimalCache.getType()).isEqualTo(CacheType.USER_DEFINED);
    }
}
