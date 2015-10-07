package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByNextPage;
import cgeo.geocaching.settings.Settings;

import rx.functions.Func1;

import android.content.Context;

public class NextPageGeocacheListLoader extends AbstractSearchLoader {
    private final SearchResult search;

    public NextPageGeocacheListLoader(final Context context, final SearchResult search) {
        super(context);
        this.search = search;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByNextPageConnectors(),
                new Func1<ISearchByNextPage, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByNextPage connector) {
                        return connector.searchByNextPage(search, Settings.isShowCaptcha(), NextPageGeocacheListLoader.this);
                    }
                });
    }

}
