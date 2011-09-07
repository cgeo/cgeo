package cgeo.geocaching.sorting;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by GC code, therefore effectively sorting by cache age
 *
 */
public class GeocodeComparator extends AbstractCacheComparator {

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return StringUtils.isNotBlank(cache1.geocode)
				&& StringUtils.isNotBlank(cache2.geocode);
	}

	@Override
	protected int compareCaches(cgCache cache1, cgCache cache2) {
		if (cache1.geocode.length() > cache2.geocode.length()) {
			return 1;
		} else if (cache2.geocode.length() > cache1.geocode.length()) {
			return -1;
		}
		return cache1.geocode.compareToIgnoreCase(cache2.geocode);
	}
}
