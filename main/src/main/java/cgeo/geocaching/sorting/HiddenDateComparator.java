package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.Date;

/**compares caches by hidden date */
class HiddenDateComparator extends AbstractDateCacheComparator {

    public static final HiddenDateComparator INSTANCE = new HiddenDateComparator();

    protected Date getCacheDate(final Geocache cache) {
        return cache.getHiddenDate();
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".hidden", sortDesc);
    }

}
