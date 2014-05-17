package cgeo.geocaching.connector.oc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.LoadFlags;

public class OkapiClientTest extends CGeoTestCase {

    public static void testGetOCCache() {
        final String geoCode = "OU0331";
        Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        assertEquals("Unexpected geo code", geoCode, cache.getGeocode());
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank");
        assertThat(cache.isDetailed()).isTrue();
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY);
        assertThat(cache).isNotNull();
        assertThat(cache.getGeocode()).isEqualTo(geoCode);
        assertThat(cache.getName()).isEqualTo("Oshkosh Municipal Tank");
        assertThat(cache.isDetailed()).isTrue();
        assertThat(cache.getOwnerDisplayName()).isNotEmpty();
        assertThat(cache.getOwnerUserId()).isEqualTo(cache.getOwnerDisplayName());
    }

    public static void testOCSearchMustWorkWithoutOAuthAccessTokens() {
        final String geoCode = "OC1234";
        Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).overridingErrorMessage("You must have a valid OKAPI key installed for running this test (but you do not need to set credentials in the app).").isNotNull();
        assertThat(cache.getName()).isEqualTo("Wupper-Schein");
    }

    public static void testOCCacheWithWaypoints() {
        final String geoCode = "OCDDD2";
        removeCacheCompletely(geoCode);
        Geocache cache = OkapiClient.getCache(geoCode);
        assertThat(cache).as("Cache from OKAPI").isNotNull();
        // cache should be stored to DB (to listID 0) when loaded above
        cache = DataStore.loadCache(geoCode, LoadFlags.LOAD_ALL_DB_ONLY);
        assertThat(cache).isNotNull();
        assertThat(cache.getWaypoints()).hasSize(3);

        // load again
        cache.refreshSynchronous(null);
        assertThat(cache.getWaypoints()).hasSize(3);
    }

}
