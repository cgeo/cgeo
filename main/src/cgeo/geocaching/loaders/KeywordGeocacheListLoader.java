package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByKeyword;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    private final @NonNull String keyword;

    public KeywordGeocacheListLoader(Context context, final @NonNull String keyword) {
        super(context);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        SearchResult searchResult = new SearchResult();

        for (ISearchByKeyword connector : ConnectorFactory.getSearchByKeywordConnectors()) {
            if (connector.isActive()) {
                searchResult.addSearchResult(connector.searchByKeyword(keyword, this));
            }
        }

        return searchResult;
    }

}
