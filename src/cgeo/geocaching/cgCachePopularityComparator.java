package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCachePopularityComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.favouriteCnt == null || cache2.favouriteCnt == null) {
				return 0;
			}
			
			if (cache1.favouriteCnt < cache2.favouriteCnt) {
				return 1;
			} else if (cache2.favouriteCnt < cache1.favouriteCnt) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCachePopularityComparator.compare: " + e.toString());
		}
		return 0;
	}
}
