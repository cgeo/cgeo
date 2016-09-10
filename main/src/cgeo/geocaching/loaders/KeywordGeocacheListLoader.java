package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByKeyword;

import android.app.Activity;
import android.support.annotation.NonNull;

import rx.functions.Func1;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, @NonNull final String keyword) {
        super(activity);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByKeywordConnectors(),
                new Func1<ISearchByKeyword, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByKeyword connector) {
                        return connector.searchByKeyword(keyword);
                    }
                });
    }

}
