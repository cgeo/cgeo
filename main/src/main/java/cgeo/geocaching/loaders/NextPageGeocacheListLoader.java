package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;

import android.app.Activity;

public class NextPageGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NextPageGeocacheListLoader(final Activity activity, final SearchResult search) {
        super(activity);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(search, ConnectorFactory.getSearchByNextPageConnectors(),
                connector -> connector.searchByNextPage(search.getConnectorContext(connector)));
    }

}
