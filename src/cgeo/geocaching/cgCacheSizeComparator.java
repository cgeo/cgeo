package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;
import java.util.ArrayList;

public class cgCacheSizeComparator implements Comparator<cgCache> {
	public static ArrayList<String> cacheSizes = new ArrayList<String>();
	
	public cgCacheSizeComparator() {
		// list sizes
		cacheSizes.add("micro");
		cacheSizes.add("small");
		cacheSizes.add("regular");
		cacheSizes.add("large");
	}

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			if (cache1.size == null || cache1.size.length() == 0 || cache2.size == null || cache2.size.length() == 0) {
				return 0;
			}

			int size1 = 0;
			int size2 = 0;
			
			int cnt = 1;
			for (String size : cacheSizes) {
				if (size.equalsIgnoreCase(cache1.size)) size1 = cnt;
				if (size.equalsIgnoreCase(cache2.size)) size2 = cnt;
				
				cnt ++;
			}
			
			if (size1 < size2) {
				return 1;
			} else if (size2 < size1) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheSizeComparator.compare: " + e.toString());
		}
		return 0;
	}
}
