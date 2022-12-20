package cgeo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import android.test.ApplicationTestCase;

import java.util.EnumSet;

public abstract class CGeoTestCase extends ApplicationTestCase<CgeoApplication> {

    public CGeoTestCase() {
        super(CgeoApplication.class);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Remove cache from DB and cache to ensure that the cache is not loaded from the database
     */
    protected static void deleteCacheFromDB(final String geocode) {
        DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL);
    }

    /**
     * remove cache from database and file system
     */
    protected static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.OWN_WAYPOINTS_ONLY_FOR_TESTING);
        DataStore.removeCache(geocode, flags);
    }

    /**
     * Remove completely the previous instance of a cache, then save this object into the database
     * and the cache cache.
     *
     * @param cache the fresh cache to save
     */
    protected static void saveFreshCacheToDB(final Geocache cache) {
        removeCacheCompletely(cache.getGeocode());
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
    }

}
