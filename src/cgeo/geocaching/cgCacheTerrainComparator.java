package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheTerrainComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.terrain == null || cache2.terrain == null) {
				return 0;
			}
			
			if (cache1.terrain > cache2.terrain) {
				return 1;
			} else if (cache2.terrain > cache1.terrain) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheTerrainComparator.compare: " + e.toString());
		}
		return 0;
	}
}
