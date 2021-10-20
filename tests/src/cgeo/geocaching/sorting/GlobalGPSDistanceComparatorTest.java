package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class GlobalGPSDistanceComparatorTest {

    @Test
    public void testCompareCaches() {
        final List<Geocache> caches = new ArrayList<>();
        for (int i = 0; i < 37; i++) {
            final Geocache cache = new Geocache();
            if (i % 3 == 0) {
                cache.setCoords(new Geopoint(i, i));
            }
            caches.add(cache);
        }
        Collections.sort(caches, new GlobalGPSDistanceComparator());
    }

}
