package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByFinder;

import android.app.Activity;
import android.support.annotation.NonNull;

import rx.functions.Func1;

public class FinderGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String username;

    public FinderGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByFinderConnectors(),
                new Func1<ISearchByFinder, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByFinder connector) {
                        return connector.searchByFinder(username);
                    }
                });
    }

}
