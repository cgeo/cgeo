package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;

import android.app.Activity;

import androidx.annotation.NonNull;

public class FinderGeocacheListLoader extends AbstractSearchLoader {

    @NonNull private final String username;

    public FinderGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        return nonEmptyCombineActive(ConnectorFactory.getSearchByFinderConnectors(),
                connector -> connector.searchByFinder(username));
    }

}
