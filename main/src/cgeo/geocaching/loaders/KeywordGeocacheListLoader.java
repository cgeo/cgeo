package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByKeyword;

import org.eclipse.jdt.annotation.NonNull;
import rx.functions.Func1;

import android.content.Context;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    private final @NonNull String keyword;

    public KeywordGeocacheListLoader(final Context context, final @NonNull String keyword) {
        super(context);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByKeywordConnectors(),
                new Func1<ISearchByKeyword, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByKeyword connector) {
                        return connector.searchByKeyword(keyword, KeywordGeocacheListLoader.this);
                    }
                });
    }

}
