package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    @NonNull public final Geopoint coords;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        //use filter search instead of dedicated distance search
        final GeocacheFilter baseFilter = GeocacheFilter.loadFromSettings();

        final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) GeocacheFilterType.DISTANCE.create();
        distanceFilter.setCoordinate(coords);

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
            connector -> connector.searchByFilter(baseFilter.and(distanceFilter)));
    }

}
