package cgeo.geocaching.loaders;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.AmendmentUtils;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.sorting.GeocacheSort;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;

import android.app.Activity;

public abstract class LiveFilterGeocacheListLoader extends AbstractSearchLoader {

    private final GeocacheSort sort;

    public LiveFilterGeocacheListLoader(final Activity activity, final GeocacheSort sort) {
        super(activity);
        this.sort = sort;
    }

    public abstract GeocacheFilterType getFilterType();

    public abstract IGeocacheFilter getAdditionalFilterParameter();

    @Override
    public SearchResult runSearch() {
        final GeocacheFilter useFilter = GeocacheFilterContext.getForType(LIVE).and(getAdditionalFilterParameter());
        final GeocacheSort sort = this.sort == null ? new GeocacheSort() : this.sort;

        final SearchResult result = nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(getFilterType()),
                connector -> connector.searchByFilter(useFilter, sort));
        AmendmentUtils.amendCachesForFilter(result, useFilter);
        return result;
    }

}
