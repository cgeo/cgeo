package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends LiveFilterGeocacheListLoader {
    @NonNull public final Geopoint coords;
    private final boolean applyNearbySearchLimit;

    public CoordsGeocacheListLoader(final Activity activity, final GeocacheSort sort, @NonNull final Geopoint coords, final boolean applyNearbySearchLimit) {
        super(activity, sort);
        this.coords = coords;
        this.applyNearbySearchLimit = applyNearbySearchLimit;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.DISTANCE;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final DistanceGeocacheFilter distanceFilter = GeocacheFilterType.DISTANCE.create();
        final int searchLimit = applyNearbySearchLimit ? Settings.getNearbySearchLimit() : Settings.getCoordinateSearchLimit();
        if (searchLimit > 0) {
            distanceFilter.setMinMaxRange(0.0f, (float) searchLimit);
        }
        distanceFilter.setCoordinate(coords);
        return distanceFilter;
    }

}
