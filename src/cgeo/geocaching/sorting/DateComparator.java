package cgeo.geocaching.sorting;

import java.util.Date;

import cgeo.geocaching.cgCache;

/**
 * compares event caches by date
 * @author bananeweizen
 *
 */
public class DateComparator extends AbstractCacheComparator {

	@Override
	protected boolean canCompare(cgCache cache1, cgCache cache2) {
		return true;
	}

	@Override
	protected int compareCaches(cgCache cache1, cgCache cache2) {
		Date event1 = null;
		Date event2 = null;
		if (cache1.isEventCache()) {
			event1 = cache1.hidden;
		}
		if (cache2.isEventCache()) {
			event2 = cache2.hidden;
		}
		if (event1 != null && event2 != null) {
			return event1.compareTo(event2);
		}
		if (event1 != null) {
			return -1;
		}
		if (event2 != null) {
			return 1;
		}
		return 0;
	}

}
