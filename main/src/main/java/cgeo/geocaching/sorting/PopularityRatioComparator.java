package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * sorts caches by popularity ratio (favorites per find in %).
 */
class PopularityRatioComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        final int finds1 = cache1.getFindsCount();
        final int finds2 = cache2.getFindsCount();

        float ratio1 = 0.0f;
        if (finds1 != 0) {
            ratio1 = (float) cache1.getFavoritePoints() / (float) finds1;
        }
        float ratio2 = 0.0f;
        if (finds2 != 0) {
            ratio2 = (float) cache2.getFavoritePoints() / (float) finds2;
        }

        return Float.compare(ratio2, ratio1);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        final int finds = cache.getFindsCount();
        return 0 == finds ? "--" : String.format(Locale.getDefault(), "%.2f", ((float) cache.getFavoritePoints()) / finds);
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        //also sort by favourite count, should resemble ratio close enough
        sql.addOrder(sql.getMainTableId() + ".favourite_cnt", !sortDesc);
    }
}
