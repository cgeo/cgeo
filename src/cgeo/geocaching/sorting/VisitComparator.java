package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by last visited date
 *
 */
public class VisitComparator extends AbstractCacheComparator {

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return cache1.visitedDate != null && cache1.visitedDate > 0
				&& cache2.visitedDate != null && cache2.visitedDate > 0;
	}

	@Override
	protected int compareCaches(cgCache cache1, cgCache cache2) {
		if (cache1.visitedDate > cache2.visitedDate) {
			return -1;
		} else if (cache1.visitedDate < cache2.visitedDate) {
			return 1;
		}
		return 0;
	}
}
