package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

public class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(Geocache cache1, Geocache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(Geocache cache1, Geocache cache2) {
        if (cache1.getUpdated() < cache2.getUpdated()) {
            return -1;
        }
        if (cache1.getUpdated() > cache2.getUpdated()) {
            return 1;
        }
        return 0;
    }

}
