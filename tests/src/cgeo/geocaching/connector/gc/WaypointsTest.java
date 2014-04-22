package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.CancellableHandler;

import android.os.Message;

public class WaypointsTest extends CGeoTestCase {

    public static final CancellableHandler handler = new CancellableHandler() {
        @Override
        protected void handleRegularMessage(final Message message) {
            // Dummy
        }
    };

    private static Geocache downloadCache(final String geocode) {
        final SearchResult searchResult = Geocache.searchByGeocode(geocode, null, 0, true, handler);
        assertThat(searchResult.getCount()).isEqualTo(1);
        return searchResult.getFirstCacheFromResult(LoadFlags.LOAD_WAYPOINTS);
    }

    public static void testDownloadWaypoints() {
        // Check that repeated loads of "GC33HXE" hold the right number of waypoints (issue #2430).
        final String GEOCODE = "GC33HXE";
        DataStore.removeCache(GEOCODE, LoadFlags.REMOVE_ALL);
        assertThat(downloadCache(GEOCODE).getWaypoints()).hasSize(9);
        assertThat(downloadCache(GEOCODE).getWaypoints()).hasSize(9);
    }

}
