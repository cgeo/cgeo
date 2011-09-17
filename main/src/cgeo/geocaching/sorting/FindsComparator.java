package cgeo.geocaching.sorting;

import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

public class FindsComparator extends AbstractCacheComparator implements
        CacheComparator {

    private cgeoapplication app;

    public FindsComparator(cgeoapplication app) {
        this.app = app;
    }

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.logCounts != null && cache2.logCounts != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        int finds1 = getFindsCount(cache1);
        int finds2 = getFindsCount(cache2);
        return finds2 - finds1;
    }

    private int getFindsCount(cgCache cache) {
        int finds = 0;
        if (cache.logCounts.isEmpty()) {
            cache.logCounts = app.loadLogCounts(cache.geocode);
        }
        Integer logged = cache.logCounts.get(cgBase.LOG_FOUND_IT);
        if (logged != null) {
            finds = logged;
        }
        return finds;
    }

}
