package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * sorts caches by popularity (favorite count)
 */
class PopularityComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return cache2.getFavoritePoints() - cache1.getFavoritePoints();
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%d", cache.getFavoritePoints());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".favourite_cnt", !sortDesc);
    }

}
