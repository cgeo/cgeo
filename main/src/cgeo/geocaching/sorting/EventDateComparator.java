package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;


/**
 * Compares caches by date. Used only for event caches.
 *
 * @author campbeb
 *
 */
public class EventDateComparator extends DateComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return super.canCompare(cache1, cache2);
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        return super.compareCaches(cache1, cache2);
    }
}
