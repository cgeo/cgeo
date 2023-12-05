package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AmendmentUtils;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;

import android.app.Activity;

import androidx.annotation.NonNull;

public class SearchFilterGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final GeocacheFilter filter;
    @NonNull private final GeocacheSort sort;

    public SearchFilterGeocacheListLoader(final Activity activity, @NonNull final GeocacheFilter filter, @NonNull final GeocacheSort sort) {
        super(activity);
        this.filter = filter;
        this.sort = sort;
    }

    @Override
    public SearchResult runSearch() {
        final SearchResult result = nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
                connector -> connector.searchByFilter(filter, sort));
        AmendmentUtils.amendCachesForFilter(result, filter);
        return result;
    }

}
