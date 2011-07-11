package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheNameComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.name == null || cache2.name == null) {
				return 0;
			}
			
			return cache1.name.compareToIgnoreCase(cache2.name);
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheNameComparator.compare: " + e.toString());
		}
		return 0;
	}
}
