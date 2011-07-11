package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheGeocodeComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.geocode == null || cache1.geocode.length() <= 0 || cache2.geocode == null || cache2.geocode.length() <= 0) {
				return 0;
			}
			
			if (cache1.geocode.length() > cache2.geocode.length()) {
				return 1;
			} else if (cache2.geocode.length() > cache1.geocode.length()) {
				return -1;
			} else {
				return cache1.geocode.compareToIgnoreCase(cache2.geocode);
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheGeocodeComparator.compare: " + e.toString());
		}
		return 0;
	}
}
