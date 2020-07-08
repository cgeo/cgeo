package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * sorts caches by distance to given position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {

    private final Geopoint coords;
    private final List<Geocache> list;
    private boolean cachedDistances;

    public static final DistanceComparator INSTANCE = new DistanceComparator();

    public DistanceComparator() {
        // This constructor should not be used as a comparator as distances will not be updated.
        // It is needed in order to really know we are sorting by Distances in the sort menu.
        // If you need it for sorting, please use the second constructor.
        coords = null;
        list = new ArrayList<>();
    }

    public DistanceComparator(final Geopoint coords, final List<Geocache> list) {
        this.coords = coords;
        // create new list so we can iterate over the list in parallel with the cache list adapter
        this.list = new ArrayList<>(list);
    }

    /**
     * calculate all distances only once to avoid costly re-calculation of the same distance during sorting
     */
    private void calculateAllDistances() {
        if (cachedDistances) {
            return;
        }
        for (final Geocache cache : list) {
            if (cache.getCoords() != null) {
                cache.setDistance(coords.distanceTo(cache.getCoords()));
            }
        }
        cachedDistances = true;
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        calculateAllDistances();
        final Float distance1 = cache1.getDistance();
        final Float distance2 = cache2.getDistance();
        if (distance1 == null) {
            return distance2 == null ? 0 : 1;
        }
        return distance2 == null ? -1 : Float.compare(distance1, distance2);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%.2f", cache.getDistance());
    }
}
