package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.location.Geopoint;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DistanceComparatorTest extends AndroidTestCase {

    public static void testCompareCaches() {
        final List<Geocache> caches = new ArrayList<Geocache>();
        for (int i = 0; i < 37; i++) {
            Geocache cache = new Geocache();
            if (i % 3 == 0) {
                cache.setCoords(new Geopoint(i, i));
            }
            caches.add(cache);
        }
        Collections.sort(caches, new DistanceComparator(Geopoint.ZERO, caches));
    }

}
