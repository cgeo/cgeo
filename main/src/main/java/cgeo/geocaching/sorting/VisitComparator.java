package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

/**
 * sorts caches by last visited date
 */
public class VisitComparator extends AbstractVisitedCacheComparator {

    public static final VisitComparator singleton = new VisitComparator();

    @Override
    protected long getCacheDate(final Geocache cache) {
        return cache.getVisitedDate();
    }

}
