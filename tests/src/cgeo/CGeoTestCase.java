package cgeo;

import cgeo.geocaching.cgData;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags;

import android.test.ApplicationTestCase;

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

}
