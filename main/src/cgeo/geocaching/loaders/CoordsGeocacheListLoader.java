package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;
import android.support.annotation.NonNull;

import rx.functions.Func1;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    @NonNull private final Geopoint coords;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByCenterConnectors(),
                new Func1<ISearchByCenter, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByCenter connector) {
                        return connector.searchByCenter(coords);
                    }
                });
    }

}
