package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class SearchFilterGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final GeocacheFilter filter;

    public SearchFilterGeocacheListLoader(final Activity activity, @NonNull final GeocacheFilter filter) {
        super(activity);
        this.filter = filter;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
                connector -> connector.searchByFilter(filter));
    }

}
