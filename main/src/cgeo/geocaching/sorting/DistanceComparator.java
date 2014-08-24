package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.ArrayList;
import java.util.List;

/**
 * sorts caches by distance to given position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {

    final private Geopoint coords;
    final private List<Geocache> list;
    private boolean cachedDistances;

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

}
