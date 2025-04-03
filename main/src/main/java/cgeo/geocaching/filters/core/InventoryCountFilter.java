package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;


public class InventoryCountFilter extends NumberRangeGeocacheFilter<Integer> {

    public InventoryCountFilter() {
        super(Integer::valueOf, Math::round);
    }

    @Override
    public Integer getValue(final Geocache cache) {
        return cache.getTrackableCount();
    }

    @Override
    protected String getSqlColumnName() {
        return DataStore.dbFieldCaches_inventoryunknown;
    }
}
