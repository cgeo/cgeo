package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LogType;

public class FindsComparator extends AbstractCacheComparator {

    private cgeoapplication app;

    public FindsComparator(cgeoapplication app) {
        this.app = app;
    }

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getLogCounts() != null && cache2.getLogCounts() != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        int finds1 = getFindsCount(cache1);
        int finds2 = getFindsCount(cache2);
        return finds2 - finds1;
    }

    private int getFindsCount(cgCache cache) {
        int finds = 0;
        if (cache.getLogCounts().isEmpty()) {
            cache.setLogCounts(app.loadLogCounts(cache.getGeocode()));
        }
        Integer logged = cache.getLogCounts().get(LogType.LOG_FOUND_IT);
        if (logged != null) {
            finds = logged.intValue();
        }
        return finds;
    }

}
