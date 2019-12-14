package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByFinder;

import android.app.Activity;

import androidx.annotation.NonNull;

import io.reactivex.functions.Function;

public class FinderGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String username;

    public FinderGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByFinderConnectors(),
                new Function<ISearchByFinder, SearchResult>() {
                    @Override
                    public SearchResult apply(final ISearchByFinder connector) {
                        return connector.searchByFinder(username);
                    }
                });
    }

}
