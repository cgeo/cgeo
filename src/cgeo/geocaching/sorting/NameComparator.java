package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by name
 *
 */
public class NameComparator extends AbstractCacheComparator {

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return cache1.name != null && cache2.name != null;
	}

	@Override
	protected int compareCaches(cgCache cache1, cgCache cache2) {
		return cache1.name.compareToIgnoreCase(cache2.name);
	}
}
