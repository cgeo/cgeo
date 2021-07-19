package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.DistanceGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.location.Geopoint;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    @NonNull public final Geopoint coords;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final DistanceGeocacheFilter distanceFilter = (DistanceGeocacheFilter) GeocacheFilterType.DISTANCE.create();
        distanceFilter.setCoordinate(coords);
        return distanceFilter;
    }

    @Override
    public SearchResult runSearch() {
        //use filter search instead of dedicated distance search
        final GeocacheFilter coordFilter = GeocacheFilterContext.getForType(LIVE).and(getAdditionalFilterParameter());

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(GeocacheFilterType.DISTANCE),
            connector -> connector.searchByFilter(coordFilter));
    }

}
