package cgeo;

import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.RemoveFlag;

import android.test.ApplicationTestCase;

import java.util.EnumSet;

public abstract class CGeoTestCase extends ApplicationTestCase<cgeoapplication> {

    public CGeoTestCase() {
        super(cgeoapplication.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
    }

    /** Remove cache from DB and cache to ensure that the cache is not loaded from the database */
    protected static void deleteCacheFromDB(String geocode) {
        cgData.removeCache(geocode, LoadFlags.REMOVE_ALL);
    }

    /**
     * remove cache from database and file system
     * 
     * @param geocode
     */
    protected static void removeCacheCompletely(final String geocode) {
        final EnumSet<RemoveFlag> flags = EnumSet.copyOf(LoadFlags.REMOVE_ALL);
        flags.add(RemoveFlag.REMOVE_OWN_WAYPOINTS_ONLY_FOR_TESTING);
        cgData.removeCache(geocode, flags);
    }

}
