package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * sorts caches by terrain rating
 */
class TerrainComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        return cache.getTerrain() != 0.0;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(cache1.getTerrain(), cache2.getTerrain());
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%.1f", cache.getTerrain());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".terrain", sortDesc);
    }

}
