package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;

import android.app.Activity;

import androidx.annotation.NonNull;

public class KeywordGeocacheListLoader extends AbstractSearchLoader {

    @NonNull public final String keyword;

    public KeywordGeocacheListLoader(final Activity activity, @NonNull final String keyword) {
        super(activity);
        this.keyword = keyword;
    }

    @Override
    public IGeocacheFilter getAdditionalFilterParameter() {
        final NameGeocacheFilter nameFilter = (NameGeocacheFilter) GeocacheFilterType.NAME.create();
        nameFilter.getStringFilter().setTextValue(keyword);
        return nameFilter;
    }

    @Override
    public SearchResult runSearch() {
        //use filter search instead of dedicated keyword search
        final GeocacheFilter useFilter = GeocacheFilterContext.getForType(LIVE).and(getAdditionalFilterParameter());

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(GeocacheFilterType.NAME),
            connector -> connector.searchByFilter(useFilter));
    }

}
