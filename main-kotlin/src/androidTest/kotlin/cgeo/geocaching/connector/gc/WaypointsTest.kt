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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.SearchResult
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WaypointsTest {

    private static Geocache downloadCache(final String geocode) {
        val searchResult: SearchResult = Geocache.searchByGeocode(geocode, null, true, null)
        assertThat(searchResult.getCount()).isEqualTo(1)
        return searchResult.getFirstCacheFromResult(LoadFlags.LOAD_WAYPOINTS)
    }

    @Test
    public Unit testDownloadWaypoints() {
        // Check that repeated loads of geocache hold the right number of waypoints (issue #2430).
        val geocode: String = "GC3KE70"
        DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL)
        assertThat(downloadCache(geocode).getWaypoints()).hasSize(15)
        assertThat(downloadCache(geocode).getWaypoints()).hasSize(15)
    }

}
