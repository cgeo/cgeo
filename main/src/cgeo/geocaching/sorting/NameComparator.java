package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by name
 *
 */
public class NameComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(Geocache cache) {
        return StringUtils.isNotBlank(cache.getName());
    }

    @Override
    protected int compareCaches(Geocache cache1, Geocache cache2) {
        return cache1.getNameForSorting().compareToIgnoreCase(cache2.getNameForSorting());
    }
}
