package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import java.util.Collection;


public class InventoryCountFilter extends NumberRangeGeocacheFilter<Integer> {

    public static InventoryCountFilter create(final Integer min, final Integer max) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.INVENTORY_COUNT, min, max);
    }

    public static InventoryCountFilter create(final Collection<Integer> values, final Integer minUnlimitedValue, final Integer maxUnlimitedValue) {
        return NumberRangeGeocacheFilter.create(GeocacheFilterType.INVENTORY_COUNT, values, minUnlimitedValue, maxUnlimitedValue);
    }

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
