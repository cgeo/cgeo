package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by number of items in inventory
 *
 * @author bananeweizen
 *
 */
public class InventoryComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final cgCache cache1, final cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        return cache2.getInventoryItems() - cache1.getInventoryItems();
    }
}
