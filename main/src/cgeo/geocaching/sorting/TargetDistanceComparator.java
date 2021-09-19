package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;

/**
 * sorts caches by distance to given target position
 *
 */
public class TargetDistanceComparator extends AbstractDistanceComparator {

    public static final TargetDistanceComparator INSTANCE = new TargetDistanceComparator();

    public static void setTargetCoords(final Geopoint coords) {
        if (coords != null) {
            INSTANCE.coords = coords;
        }
    }
}
