package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends LiveFilterGeocacheListLoader {
    @NonNull public final Geopoint coords;
    private final boolean applyNearbySearchLimit;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords, final boolean applyNearbySearchLimit) {
        super(activity);
        this.coords = coords;
        this.applyNearbySearchLimit = applyNearbySearchLimit;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.DISTANCE;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) GeocacheFilterType.DISTANCE.create();
        if (applyNearbySearchLimit) {
            final int nearbySearchLimit = Settings.getNearbySearchLimit();
            if (nearbySearchLimit > 0) {
                distanceFilter.setMinMaxRange(0.0f, (float) nearbySearchLimit);
            }
        }
        distanceFilter.setCoordinate(coords);
        return distanceFilter;
    }

}
