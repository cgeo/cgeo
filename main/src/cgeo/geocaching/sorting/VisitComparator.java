package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CalendarUtils;

import androidx.annotation.NonNull;

/**
 * sorts caches by last visited date
 */
public class VisitComparator extends AbstractCacheComparator {

    public static final VisitComparator singleton = new VisitComparator();

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return compare(cache2.getVisitedDate(), cache1.getVisitedDate());
    }

    /**
     * copy of Long#compare to avoid boxing
     */
    public static int compare(final long lhs, final long rhs) {
        return Long.compare(lhs, rhs);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return CalendarUtils.yearMonth(cache.getVisitedDate());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".visiteddate", !sortDesc);
    }

}
