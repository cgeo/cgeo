package cgeo.geocaching.loaders;

import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends LiveFilterGeocacheListLoader {
    @NonNull public final Geopoint coords;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public GeocacheFilterType getFilterType() {
        return GeocacheFilterType.DISTANCE;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) GeocacheFilterType.DISTANCE.create();
        distanceFilter.setCoordinate(coords);
        return distanceFilter;
    }


}
