package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

import java.util.Date;

/**compares caches by hidden date */
class LastFoundComparator extends AbstractDateCacheComparator {

    public static final CacheComparator INSTANCE_INVERSE = new InverseComparator(new LastFoundComparator());

    protected Date getCacheDate(final Geocache cache) {
        return cache.getLastFound();
    }

}
