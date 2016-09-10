package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByOwner;

import android.app.Activity;
import android.support.annotation.NonNull;

import rx.functions.Func1;

public class OwnerGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String username;

    public OwnerGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByOwnerConnectors(),
                new Func1<ISearchByOwner, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByOwner connector) {
                        return connector.searchByOwner(username);
            }
        });
    }

}
