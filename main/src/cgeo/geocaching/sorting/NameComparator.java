package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by name
 *
 */
public class NameComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return StringUtils.isNotBlank(cache1.name) && StringUtils.isNotBlank(cache2.name);
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        return cache1.getNameForSorting().compareToIgnoreCase(cache2.getNameForSorting());
    }
}
