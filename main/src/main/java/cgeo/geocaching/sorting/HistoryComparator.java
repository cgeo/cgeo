package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

/**
 * sorts caches by history date (newer of last visited date and date of offline log)
 */
public class HistoryComparator extends AbstractVisitedCacheComparator {

    public static final HistoryComparator singleton = new HistoryComparator();

    @Override
    protected long getCacheDate(final Geocache cache) {
        return cache.getHistoryDate();
    }

}
