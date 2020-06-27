package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Long.compare(cache1.getUpdated(), cache2.getUpdated());
    }

}
