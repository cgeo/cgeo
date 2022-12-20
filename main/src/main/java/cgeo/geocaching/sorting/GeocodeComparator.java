package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

/**
 * sorts caches by geo code, therefore effectively sorting by cache age
 */
public class GeocodeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final Geocache cache) {
        // This will fall back to geocode comparisons.
        return false;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        throw new IllegalStateException("should never be called");
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return cache.getGeocode();
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".geocode", sortDesc);
    }

}
