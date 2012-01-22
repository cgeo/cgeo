package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.geopoint.Geopoint;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DistanceComparatorTest extends AndroidTestCase {

    public static void testCompareCaches() {
        final List<cgCache> caches = new ArrayList<cgCache>();
        for (int i = 0; i < 37; i++) {
            cgCache cache = new cgCache();
            if (i % 3 == 0) {
                cache.setCoords(new Geopoint(i, i));
            }
            caches.add(cache);
        }
        Collections.sort(caches, new DistanceComparator(new Geopoint(0, 0), caches));
    }

}
