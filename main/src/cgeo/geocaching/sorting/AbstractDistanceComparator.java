package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * sorts caches by distance to given position
 */
public abstract class AbstractDistanceComparator extends AbstractCacheComparator {

    protected Geopoint coords = Geopoint.ZERO; // will be overwritten

    @Override
    protected void beforeSort(final List<Geocache> list) {
        super.beforeSort(list);
        // calculate all distances only once to avoid costly re-calculation of the same distance during sorting
        for (final Geocache cache : list) {
            if (cache.getCoords() != null) {
                cache.setDistance(coords.distanceTo(cache.getCoords()));
            }
        }
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        final Float distance1 = cache1.getDistance();
        final Float distance2 = cache2.getDistance();
        if (distance1 == null) {
            return distance2 == null ? 0 : 1;
        }
        return distance2 == null ? -1 : Float.compare(distance1, distance2);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return Units.getDistanceFromKilometers(cache.getDistance());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(DataStore.getSqlDistanceSquare(sql.getMainTableId(), coords), sortDesc);
    }

}
