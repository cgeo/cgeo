package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByFinder;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;

public class FinderGeocacheListLoader extends AbstractSearchLoader {

    private final @NonNull String username;

    public FinderGeocacheListLoader(final Context context, final @NonNull String username) {
        super(context);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        SearchResult searchResult = new SearchResult();

        for (ISearchByFinder connector : ConnectorFactory.getSearchByFinderConnectors()) {
            if (connector.isActive()) {
                searchResult.addSearchResult(connector.searchByFinder(username, this));
            }
        }

        return searchResult;
    }

}
