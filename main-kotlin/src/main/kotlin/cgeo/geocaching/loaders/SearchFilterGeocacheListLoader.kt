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
import cgeo.geocaching.sorting.GeocacheSort

import android.app.Activity

import androidx.annotation.NonNull

class SearchFilterGeocacheListLoader : AbstractSearchLoader() {

    private final GeocacheFilter filter
    private final GeocacheSort sort

    public SearchFilterGeocacheListLoader(final Activity activity, final GeocacheFilter filter, final GeocacheSort sort) {
        super(activity)
        this.filter = filter
        this.sort = sort
    }

    override     public SearchResult runSearch() {
        val result: SearchResult = nonEmptyCombineActive(ConnectorFactory.getSearchByFilterConnectors(),
                connector -> connector.searchByFilter(filter, sort))
        AmendmentUtils.amendCachesForFilter(result, filter)
        return result
    }

}
