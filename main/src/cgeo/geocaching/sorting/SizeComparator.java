package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

/**
 * sorts caches by size
 *
 */
class SizeComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getSize().comparable - cache1.getSize().comparable;
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return cache.getSize().toString();
    }
}
