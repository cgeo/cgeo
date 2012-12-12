package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Geopoint;

import java.util.List;

/**
 * sorts caches by distance to given position
 *
 */
public class DistanceComparator extends AbstractCacheComparator {

    final private Geopoint coords;
    final private List<cgCache> list;
    private boolean cachedDistances;

    public DistanceComparator(final Geopoint coords, List<cgCache> list) {
        this.coords = coords;
        this.list = list;
    }

    /**
     * calculate all distances only once to avoid costly re-calculation of the same distance during sorting
     */
    private void calculateAllDistances() {
        if (cachedDistances) {
            return;
        }
        for (cgCache cache : list) {
            if (cache.getCoords() != null) {
                cache.setDistance(coords.distanceTo(cache.getCoords()));
            }
            else {
                cache.setDistance(null);
            }
        }
        cachedDistances = true;
    }

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        calculateAllDistances();
        if (cache1.getCoords() == null && cache2.getCoords() == null) {
            return 0;
        }
        if (cache1.getCoords() == null) {
            return 1;
        }
        if (cache2.getCoords() == null) {
            return -1;
        }
        return Float.compare(cache1.getDistance(), cache2.getDistance());
    }

}
