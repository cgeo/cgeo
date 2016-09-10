package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByNextPage;

import android.app.Activity;

import rx.functions.Func1;

public class NextPageGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NextPageGeocacheListLoader(final Activity activity, final SearchResult search) {
        super(activity);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByNextPageConnectors(),
                new Func1<ISearchByNextPage, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByNextPage connector) {
                        return connector.searchByNextPage(search);
                    }
                });
    }

}
