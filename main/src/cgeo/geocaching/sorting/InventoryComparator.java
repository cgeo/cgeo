package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by number of items in inventory
 */
public class InventoryComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache1, final Geocache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getInventoryItems() - cache1.getInventoryItems();
    }
}
