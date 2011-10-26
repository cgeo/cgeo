package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

import java.util.Date;

/**
 * compares caches by date
 *
 * @author bananeweizen
 *
 */
public class DateComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        Date date1 = cache1.getHidden();
        Date date2 = cache2.getHidden();
        if (date1 != null && date2 != null) {
            return date1.compareTo(date2);
        }
        if (date1 != null) {
            return -1;
        }
        if (date2 != null) {
            return 1;
        }
        return 0;
    }
}
