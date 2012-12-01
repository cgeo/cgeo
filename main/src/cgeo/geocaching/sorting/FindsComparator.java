package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.LogType;

public class FindsComparator extends AbstractCacheComparator {

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

    private static int getFindsCount(cgCache cache) {
        if (cache.getLogCounts().isEmpty()) {
            cache.setLogCounts(cgData.loadLogCounts(cache.getGeocode()));
        }
        Integer logged = cache.getLogCounts().get(LogType.FOUND_IT);
        if (logged != null) {
            return logged;
        }
        return 0;
    }

}
