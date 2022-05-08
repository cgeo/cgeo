package cgeo.geocaching.sorting;

import cgeo.geocaching.location.Geopoint;

import androidx.annotation.NonNull;

/**
 * sorts caches by distance to given target position
 */
public class TargetDistanceComparator extends AbstractDistanceComparator {

    public TargetDistanceComparator(@NonNull final Geopoint coords) {
        this.coords = coords;
    }
}
