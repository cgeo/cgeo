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
import cgeo.geocaching.connector.ConnectorFactory

import android.app.Activity

class NextPageGeocacheListLoader : AbstractSearchLoader() {
    private final SearchResult search

    public NextPageGeocacheListLoader(final Activity activity, final SearchResult search) {
        super(activity)
        this.search = search
    }

    override     public SearchResult runSearch() {
        return SearchResult.parallelCombineActive(search, ConnectorFactory.getSearchByNextPageConnectors(),
                connector -> connector.searchByNextPage(search.getConnectorContext(connector)))
    }

}
