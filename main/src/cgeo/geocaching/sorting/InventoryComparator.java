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
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        int itemCount1 = cache1.inventoryItems;
        int itemCount2 = cache2.inventoryItems;
        if (itemCount1 < itemCount2) {
            return 1;
        } else if (itemCount2 < itemCount1) {
            return -1;
        }
        return 0;
    }
}
