package cgeo.geocaching.connector.gc;

import cgeo.CGeoTestCase;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.utils.CancellableHandler;

import android.os.Message;

public class WaypointsTest extends CGeoTestCase {

    public static CancellableHandler handler = new CancellableHandler() {
        @Override
        protected void handleRegularMessage(final Message message) {
            // Dummy
        }
    };

    private static cgCache downloadCache(final String geocode) {
        final SearchResult searchResult = cgCache.searchByGeocode(geocode, null, 0, true, handler);
        assertEquals(1, searchResult.getCount());
        return searchResult.getFirstCacheFromResult(LoadFlags.LOAD_WAYPOINTS);
    }

    public static void testDownloadWaypoints() {
        // Check that repeated loads of "GC33HXE" hold the right number of waypoints (issue #2430).
        final String GEOCODE = "GC33HXE";
        cgData.removeCache(GEOCODE, LoadFlags.REMOVE_ALL);
        assertEquals(9, downloadCache(GEOCODE).getWaypoints().size());
        assertEquals(9, downloadCache(GEOCODE).getWaypoints().size());
    }

}
