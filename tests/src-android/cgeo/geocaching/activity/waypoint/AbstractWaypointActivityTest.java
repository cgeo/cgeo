package cgeo.geocaching.activity.waypoint;

import cgeo.geocaching.EditWaypointActivity_;
import cgeo.geocaching.activity.AbstractEspressoTest;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import java.util.Collections;

import static org.assertj.core.api.Java6Assertions.assertThat;

public abstract class AbstractWaypointActivityTest extends AbstractEspressoTest<EditWaypointActivity_> {

    private Geocache cache;

    public AbstractWaypointActivityTest() {
        super(EditWaypointActivity_.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = createTestCache();
        DataStore.saveCache(cache, Collections.singleton(SaveFlag.CACHE));
    }

    @Override
    protected void tearDown() throws Exception {
        removeTestCache();
        super.tearDown();
    }

    protected final Geocache getCache() {
        return cache;
    }

    private void removeTestCache() {
        DataStore.removeCache(cache.getGeocode(), LoadFlags.REMOVE_ALL);
        assertThat(DataStore.loadCache(cache.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)).isNull();
    }

    protected Geocache createTestCache() {
        final Geocache testCache = new Geocache();
        testCache.setGeocode("TEST");
        testCache.setType(CacheType.TRADITIONAL);
        return testCache;
    }
}
