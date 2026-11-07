package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CalendarUtils;

import androidx.annotation.NonNull;

/**
 * sorts caches by a visited date, latest first
 */
abstract class AbstractVisitedCacheComparator extends AbstractCacheComparator {

    protected abstract long getCacheDate(Geocache cache);

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return compare(getCacheDate(cache2), getCacheDate(cache1));
    }

    /**
     * copy of Long#compare to avoid boxing
     */
    public static int compare(final long lhs, final long rhs) {
        return Long.compare(lhs, rhs);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return CalendarUtils.yearMonth(getCacheDate(cache));
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".visiteddate", !sortDesc);
    }

}
