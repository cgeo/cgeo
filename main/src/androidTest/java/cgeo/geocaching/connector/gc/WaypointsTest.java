package cgeo.geocaching.connector.gc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaypointsTest extends CGeoTestCase {

    private static Geocache downloadCache(final String geocode) {
        final SearchResult searchResult = Geocache.searchByGeocode(geocode, null, true, null);
        assertThat(searchResult.getCount()).isEqualTo(1);
        return searchResult.getFirstCacheFromResult(LoadFlags.LOAD_WAYPOINTS);
    }

    public static void testDownloadWaypoints() {
        // Check that repeated loads of geocache hold the right number of waypoints (issue #2430).
        final String geocode = "GC3KE70";
        DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL);
        assertThat(downloadCache(geocode).getWaypoints()).hasSize(15);
        assertThat(downloadCache(geocode).getWaypoints()).hasSize(15);
    }

}
