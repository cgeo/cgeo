package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

import androidx.annotation.NonNull;

public class CoordsGeocacheListLoader extends AbstractSearchLoader {
    @NonNull private final Geopoint coords;

    public CoordsGeocacheListLoader(final Activity activity, @NonNull final Geopoint coords) {
        super(activity);
        this.coords = coords;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByCenterConnectors(),
                connector -> connector.searchByCenter(coords));
    }

}
