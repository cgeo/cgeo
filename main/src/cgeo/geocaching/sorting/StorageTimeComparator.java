package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        if (cache1.getUpdated() < cache2.getUpdated()) {
            return -1;
        }
        if (cache1.getUpdated() > cache2.getUpdated()) {
            return 1;
        }
        return 0;
    }

}
