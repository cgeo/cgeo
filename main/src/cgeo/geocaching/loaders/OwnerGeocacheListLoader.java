package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.capability.ISearchByOwner;

import org.eclipse.jdt.annotation.NonNull;
import rx.functions.Func1;

import android.content.Context;

public class OwnerGeocacheListLoader extends AbstractSearchLoader {

    private final @NonNull String username;

    public OwnerGeocacheListLoader(final Context context, final @NonNull String username) {
        super(context);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(ConnectorFactory.getSearchByOwnerConnectors(),
                new Func1<ISearchByOwner, SearchResult>() {
                    @Override
                    public SearchResult call(final ISearchByOwner connector) {
                        return connector.searchByOwner(username, OwnerGeocacheListLoader.this);
            }
        });
    }

}
