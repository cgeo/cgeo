package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;

import android.app.Activity;

public abstract class LiveFilterGeocacheListLoader extends AbstractSearchLoader {

    public LiveFilterGeocacheListLoader(final Activity activity) {
        super(activity);
    }

    public abstract GeocacheFilterType getFilterType();

    public abstract IGeocacheFilter getAdditionalFilterParameter();

    @Override
    public SearchResult runSearch() {
        final GeocacheFilter useFilter = GeocacheFilterContext.getForType(LIVE).and(getAdditionalFilterParameter());

        return nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(getFilterType()),
                connector -> connector.searchByFilter(useFilter));
    }

}
