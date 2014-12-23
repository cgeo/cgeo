package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by name
 *
 */
class NameComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        return StringUtils.isNotBlank(cache.getName());
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache1.getNameForSorting().compareToIgnoreCase(cache2.getNameForSorting());
    }
}
