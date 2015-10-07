package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by number of items in inventory
 */
class InventoryComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getInventoryItems() - cache1.getInventoryItems();
    }
}
