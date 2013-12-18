package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByCenter;
import cgeo.geocaching.geopoint.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    private final @NonNull Geopoint coords;

    public CoordsGeocacheListLoader(final Context context, final @NonNull Geopoint coords) {
        super(context);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {

        SearchResult search = new SearchResult();

        for (ISearchByCenter centerConn : ConnectorFactory.getSearchByCenterConnectors()) {
            if (centerConn.isActive()) {
                search.addSearchResult(centerConn.searchByCenter(coords, this));
            }
        }

        return search;
    }

}
