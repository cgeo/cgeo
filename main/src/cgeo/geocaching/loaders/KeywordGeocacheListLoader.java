package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NameGeocacheFilter;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    @NonNull public final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, @NonNull final String keyword) {
        super(activity);
        this.keyword = keyword;
    }

    @Override
    public SearchResult runSearch() {
        //use filter search instead of dedicated keyword search
        final GeocacheFilter baseFilter = GeocacheFilter.loadFromSettings();

        final NameGeocacheFilter nameFilter = (NameGeocacheFilter) GeocacheFilterType.NAME.create();
        nameFilter.getStringFilter().setTextValue(keyword);

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
            connector -> connector.searchByFilter(baseFilter.and(nameFilter)));
    }

}
