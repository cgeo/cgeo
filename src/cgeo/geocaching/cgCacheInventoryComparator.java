package cgeo.geocaching;

import java.util.Comparator;
import android.util.Log;

/**
 * compares by number of items in inventory
 * @author bananeweizen
 *
 */
public class cgCacheInventoryComparator implements Comparator<cgCache> {

	public int compare(cgCache cache1, cgCache cache2) {
		try {
			int itemCount1 = 0;
			int itemCount2 = 0;
			if (cache1.difficulty != null) {
				itemCount1 = cache1.inventoryItems;
			}
			if (cache2.difficulty != null) {
				itemCount2 = cache2.inventoryItems;
			}

			if (itemCount1 < itemCount2) {
				return 1;
			} else if (itemCount2 < itemCount1) {
				return -1;
			} else {
				return 0;
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgCacheInventoryComparator.compare: " + e.toString());
		}
		return 0;
	}
}
