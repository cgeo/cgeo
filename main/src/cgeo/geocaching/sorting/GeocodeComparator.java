package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by GC code, therefore effectively sorting by cache age
 *
 */
public class GeocodeComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return StringUtils.isNotBlank(cache1.getGeocode())
                && StringUtils.isNotBlank(cache2.getGeocode());
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        if (cache1.getGeocode().length() > cache2.getGeocode().length()) {
            return 1;
        } else if (cache2.getGeocode().length() > cache1.getGeocode().length()) {
            return -1;
        }
        return cache1.getGeocode().compareToIgnoreCase(cache2.getGeocode());
    }
}
