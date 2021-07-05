package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.OwnerGeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class OwnerGeocacheListLoader extends AbstractSearchLoader {

    @NonNull public final String username;

    public OwnerGeocacheListLoader(final Activity activity, @NonNull final String username) {
        super(activity);
        this.username = username;
    }

    @Override
    public SearchResult runSearch() {
        //use filter search instead of dedicated owner search
        final GeocacheFilter baseFilter = GeocacheFilter.loadFromSettings();

        final OwnerGeocacheFilter ownerFilter = (OwnerGeocacheFilter) GeocacheFilterType.OWNER.create();
        ownerFilter.getStringFilter().setTextValue(username);

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
            connector -> connector.searchByFilter(baseFilter.and(ownerFilter)));
    }

}
