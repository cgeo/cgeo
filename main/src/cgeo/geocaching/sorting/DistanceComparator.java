package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Geopoint;

/**
 * sorts caches by distance to given position
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
        if ((cache1.getCoords() == null || cache2.getCoords() == null)
                && cache1.getDistance() != null && cache2.getDistance() != null) {
            return Double.compare(cache1.getDistance(), cache2.getDistance());
        } else {
            if (cache1.getCoords() == null) {
                return 1;
            }
            if (cache2.getCoords() == null) {
                return -1;
            }

            return Float.compare(coords.distanceTo(cache1.getCoords()),
                    coords.distanceTo(cache2.getCoords()));
        }
    }

}
