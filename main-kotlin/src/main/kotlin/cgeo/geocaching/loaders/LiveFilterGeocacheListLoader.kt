// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.loaders

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.AmendmentUtils
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.sorting.GeocacheSort
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE

import android.app.Activity

abstract class LiveFilterGeocacheListLoader : AbstractSearchLoader() {

    private final GeocacheSort sort

    public LiveFilterGeocacheListLoader(final Activity activity, final GeocacheSort sort) {
        super(activity)
        this.sort = sort
    }

    public abstract GeocacheFilterType getFilterType()

    public abstract IGeocacheFilter getAdditionalFilterParameter()

    override     public SearchResult runSearch() {
        val useFilter: GeocacheFilter = GeocacheFilterContext.getForType(LIVE).and(getAdditionalFilterParameter())
        val sort: GeocacheSort = this.sort == null ? GeocacheSort() : this.sort

        val result: SearchResult = nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(getFilterType()),
                connector -> connector.searchByFilter(useFilter, sort))
        AmendmentUtils.amendCachesForFilter(result, useFilter)
        return result
    }

}
