package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import java.util.ArrayList;
import java.util.Date;

/**
 * compares caches by date
 */
public class DateComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        final Date date1 = cache1.getHiddenDate();
        final Date date2 = cache2.getHiddenDate();
        if (date1 != null && date2 != null) {
            final int dateDifference = date1.compareTo(date2);
            // for equal dates, sort by distance
            if (dateDifference == 0) {
                final ArrayList<cgCache> list = new ArrayList<cgCache>();
                list.add(cache1);
                list.add(cache2);
                final DistanceComparator distanceComparator = new DistanceComparator(cgeoapplication.getInstance().currentGeo().getCoords(), list);
                return distanceComparator.compare(cache1, cache2);
            }
            return dateDifference;
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
