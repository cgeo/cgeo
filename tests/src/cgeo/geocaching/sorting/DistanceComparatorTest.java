package cgeo.geocaching.sorting;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.location.Geopoint;

public class DistanceComparatorTest extends TestCase {

    public static void testCompareCaches() {
        final List<Geocache> caches = new ArrayList<>();
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
