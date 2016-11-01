package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByNextPage;

import android.app.Activity;

import io.reactivex.functions.Function;

public class NextPageGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NextPageGeocacheListLoader(final Activity activity, final SearchResult search) {
        super(activity);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByNextPageConnectors(),
                new Function<ISearchByNextPage, SearchResult>() {
                    @Override
                    public SearchResult apply(final ISearchByNextPage connector) {
                        return connector.searchByNextPage(search);
                    }
                });
    }

}
