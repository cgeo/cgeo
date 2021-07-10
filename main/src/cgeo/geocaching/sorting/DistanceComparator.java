package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * sorts caches by distance to given position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {

    private Geopoint coords;

    private static AtomicLong gpsPosVersion = new AtomicLong(0);
    public static final DistanceComparator DISTANCE_TO_GLOBAL_GPS = new DistanceComparator(Geopoint.ZERO);

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

    public static void updateGlobalGps(final Geopoint gpsPosition) {
        if (gpsPosition != null) {
            DISTANCE_TO_GLOBAL_GPS.coords = gpsPosition;
            gpsPosVersion.incrementAndGet();
        }
    }

    public DistanceComparator(final Geopoint coords) {
        this.coords = coords == null ? Geopoint.ZERO : coords;
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
        //sql.addOrder(DataStore.getCoordDiffExpression(coords, sql.getMainTableId()), sortDesc);
        sql.addOrder(DataStore.getSqlDistanceSquare(sql.getMainTableId(), coords), sortDesc);
    }

}
