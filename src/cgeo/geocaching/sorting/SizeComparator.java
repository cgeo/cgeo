package cgeo.geocaching.sorting;

import org.apache.commons.lang3.StringUtils;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by size
 *
 */
public class SizeComparator extends AbstractCacheComparator {

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return StringUtils.isNotBlank(cache1.size) && StringUtils.isNotBlank(cache2.size);
	}

	@Override
	protected int compareCaches(cgCache cache1, cgCache cache2) {
		int size1 = getSize(cache1);
		int size2 = getSize(cache2);
		if (size1 < size2) {
			return 1;
		} else if (size2 < size1) {
			return -1;
		}
		return 0;
	}

	/**
	 * speed optimized comparison of size string
	 * @param cache
	 * @return
	 */
	private static int getSize(final cgCache cache) {
		char c = cache.size.charAt(0);
		switch (c) {
		case 'm': // micro
			return 1;
		case 's': // small
			return 2;
		case 'r': // regular
			return 3;
		case 'l': // large
			return 4;
		default:
			return 0;
		}
	}
}