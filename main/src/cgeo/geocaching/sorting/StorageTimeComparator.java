package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

public class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getUpdated() < cache2.getUpdated()) {
            return -1;
        }
        if (cache1.getUpdated() > cache2.getUpdated()) {
            return 1;
        }
        return 0;
    }

}
