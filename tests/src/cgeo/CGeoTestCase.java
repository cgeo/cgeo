package cgeo;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import android.test.ApplicationTestCase;

import java.util.EnumSet;

public abstract class CGeoTestCase extends ApplicationTestCase<CgeoApplication> {

    private boolean oldStoreMapsFlag;
    private boolean oldStoreWpMapsFlag;
    private boolean oldMapStoreFlagsRecorded = false;

    public CGeoTestCase() {
        super(CgeoApplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
    }

    /** Remove cache from DB and cache to ensure that the cache is not loaded from the database */
    protected static void deleteCacheFromDB(String geocode) {
        DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL);
    }

    /**
     * remove cache from database and file system
     *
     * @param geocode
     */
    protected static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.REMOVE_OWN_WAYPOINTS_ONLY_FOR_TESTING);
        DataStore.removeCache(geocode, flags);
    }

    /**
     * must be called once before setting the flags
     * can be called again after restoring the flags
     */
    protected void recordMapStoreFlags() {
        if (oldMapStoreFlagsRecorded) {
            throw new IllegalStateException("MapStoreFlags already recorded!");
        }
        oldStoreMapsFlag = Settings.isStoreOfflineMaps();
        oldStoreWpMapsFlag = Settings.isStoreOfflineWpMaps();
        oldMapStoreFlagsRecorded = true;
    }

    /**
     * can be called after recordMapStoreFlags,
     * to set the flags for map storing as necessary
     * @param storeCacheMap
     * @param storeWpMaps
     */
    protected void setMapStoreFlags(boolean storeCacheMap, boolean storeWpMaps) {
        if (!oldMapStoreFlagsRecorded) {
            throw new IllegalStateException("Previous MapStoreFlags havn't been recorded! Setting not allowed");
        }

        TestSettings.setStoreOfflineMaps(storeCacheMap);
        TestSettings.setStoreOfflineWpMaps(storeWpMaps);
    }

    /**
     * has to be called after completion of the test (preferably in the finally part of a try statement)
     */
    protected void restoreMapStoreFlags() {
        if (!oldMapStoreFlagsRecorded) {
            throw new IllegalStateException("Previous MapStoreFlags havn't been recorded. Restore not possible");
        }

        TestSettings.setStoreOfflineMaps(oldStoreMapsFlag);
        TestSettings.setStoreOfflineWpMaps(oldStoreWpMapsFlag);
        oldMapStoreFlagsRecorded = false;
    }
}
