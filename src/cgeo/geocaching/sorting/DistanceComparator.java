package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * sorts caches by distance to current position
 * 
 */
public class DistanceComparator extends AbstractCacheComparator {
    private final Geopoint coords;

    public DistanceComparator(final Geopoint coords) {
        this.coords = coords;
    }

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        if ((cache1.coords == null || cache2.coords == null)
                && cache1.distance != null && cache2.distance != null) {
            return Double.compare(cache1.distance, cache2.distance);
        } else {
            if (cache1.coords == null) {
                return 1;
            }
            if (cache2.coords == null) {
                return -1;
            }

            return Float.compare(coords.distanceTo(cache1.coords),
                    coords.distanceTo(cache2.coords));
        }
    }

}
