package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CalendarUtils;

import androidx.annotation.NonNull;

class StorageTimeComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        //Note: this comparator sorts by "time of last update" (low to high)
        // -> which is transferred in GUI to "storage time" (high to low)
        return Long.compare(cache1.getUpdated(), cache2.getUpdated());
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getUpdated());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".updated", sortDesc);
    }
}
