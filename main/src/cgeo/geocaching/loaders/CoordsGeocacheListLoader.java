package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

import rx.functions.Func1;

import android.content.Context;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    private final @NonNull Geopoint coords;

    public CoordsGeocacheListLoader(final Context context, final @NonNull Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByCenterConnectors(),
                new Func1<ISearchByCenter, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByCenter connector) {
                        return connector.searchByCenter(coords, CoordsGeocacheListLoader.this);
                    }
                });
    }

}
