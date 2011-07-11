package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheRatingComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			Float rating1 = cache1.rating;
			Float rating2 = cache2.rating;
			if (rating1 == null || rating2 == null) {
				return 0;
			}

			// voting can be disabled for caches, then assume an average rating instead
			if (rating1 == 0.0) {
				rating1 = 2.5f;
			}
			if (rating2 == 0.0) {
				rating2 = 2.5f;
			}

			if (rating1 < rating2) {
				return 1;
			} else if (rating2 < rating1) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheRatingComparator.compare: " + e.toString());
		}
		return 0;
	}
}
