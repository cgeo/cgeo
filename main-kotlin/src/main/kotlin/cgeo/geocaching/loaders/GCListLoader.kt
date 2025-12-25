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
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCParser
import cgeo.geocaching.models.GCList
import cgeo.geocaching.settings.Settings

import android.app.Activity

import java.util.List

class GCListLoader : AbstractSearchLoader() {
    private final List<GCList> gcLists

    public GCListLoader(final Activity activity, final List<GCList> gcLists) {
        super(activity)
        this.gcLists = gcLists
    }

    override     public SearchResult runSearch() {
        if (Settings.isGCConnectorActive()) {
            val combinedResult: SearchResult = SearchResult()
            for (final GCList gcList : gcLists) {
                if (gcList.isBookmarkList()) {
                    val bmResult: SearchResult = GCParser.searchByBookmarkList(GCConnector.getInstance(), gcList.getGuid(), 0)
                    combinedResult.addSearchResult(bmResult)
                } else {
                    val pqResult: SearchResult = GCParser.searchByPocketQuery(GCConnector.getInstance(), gcList.getShortGuid(), gcList.getPqHash())
                    combinedResult.addSearchResult(pqResult)
                }
            }
            return combinedResult
        }

        return SearchResult()
    }
}
