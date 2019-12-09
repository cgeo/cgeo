package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByOwner;

import android.app.Activity;

import androidx.annotation.NonNull;

import io.reactivex.functions.Function;

public class OwnerGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String username;

    public OwnerGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByOwnerConnectors(),
                new Function<ISearchByOwner, SearchResult>() {
                    @Override
                    public SearchResult apply(final ISearchByOwner connector) {
                        return connector.searchByOwner(username);
            }
        });
    }

}
