package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

public class cgCacheDifficultyComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.difficulty == null || cache2.difficulty == null) {
				return 0;
			}
			
			if (cache1.difficulty > cache2.difficulty) {
				return 1;
			} else if (cache2.difficulty > cache1.difficulty) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheDifficultyComparator.compare: " + e.toString());
		}
		return 0;
	}
}
