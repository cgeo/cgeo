package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.utils.TextUtils;

import org.apache.commons.lang3.StringUtils;

import java.text.Collator;

/**
 * sorts caches by name
 *
 */
class NameComparator extends AbstractCacheComparator {

    private final Collator collator = TextUtils.getCollator();

    @Override
    protected boolean canCompare(final Geocache cache) {
        return StringUtils.isNotBlank(cache.getName());
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return collator.compare(cache1.getNameForSorting(), cache2.getNameForSorting());
    }
}
