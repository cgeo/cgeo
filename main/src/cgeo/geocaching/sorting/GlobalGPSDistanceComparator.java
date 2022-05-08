package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;

import java.util.concurrent.atomic.AtomicLong;

/**
 * sorts caches by distance to given GPS position
 */
public class GlobalGPSDistanceComparator extends AbstractDistanceComparator {

    private static final AtomicLong gpsPosVersion = new AtomicLong(0);
    public static final GlobalGPSDistanceComparator INSTANCE = new GlobalGPSDistanceComparator();

    public static void updateGlobalGps(final Geopoint gpsPosition) {
        if (gpsPosition != null) {
            INSTANCE.coords = gpsPosition;
            gpsPosVersion.incrementAndGet();
        }
    }
}
