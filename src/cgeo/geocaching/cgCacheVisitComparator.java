package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheVisitComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.visitedDate == null || cache1.visitedDate <= 0 || cache2.visitedDate == null || cache2.visitedDate <= 0) {
				return 0;
			}

			if (cache1.visitedDate > cache2.visitedDate) {
				return -1;
			} else if (cache1.visitedDate < cache2.visitedDate) {
				return 1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheVisitComparator.compare: " + e.toString());
		}

		return 0;
	}
}
