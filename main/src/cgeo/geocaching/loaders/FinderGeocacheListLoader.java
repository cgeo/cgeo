package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByFinder;

import org.eclipse.jdt.annotation.NonNull;
import rx.functions.Func1;

import android.content.Context;

public class FinderGeocacheListLoader extends AbstractSearchLoader {

    private final @NonNull String username;

    public FinderGeocacheListLoader(final Context context, final @NonNull String username) {
        super(context);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByFinderConnectors(),
                new Func1<ISearchByFinder, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByFinder connector) {
                        return connector.searchByFinder(username, FinderGeocacheListLoader.this);
                    }
                });
    }

}
