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

import cgeo.geocaching.Intents
import cgeo.geocaching.SearchResult
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.sorting.CacheComparator
import cgeo.geocaching.storage.DataStore

import android.app.Activity
import android.os.Bundle

class OfflineGeocacheListLoader : AbstractSearchLoader() {

    private final Int listId
    private final Geopoint searchCenter
    private final GeocacheFilter filter

    private final CacheComparator sort
    private final Boolean sortInverse
    private final Int limit

    public OfflineGeocacheListLoader(final Activity activity, final Geopoint searchCenter, final Int listId, final GeocacheFilter filter, final CacheComparator sort, final Boolean sortInverse, final Int limit) {
        super(activity)
        this.searchCenter = searchCenter
        this.listId = listId
        this.filter = filter
        this.sort = sort
        this.sortInverse = sortInverse
        this.limit = limit
    }

    override     public SearchResult runSearch() {
        return DataStore.getBatchOfStoredCaches(searchCenter, listId, filter, sort, sortInverse, limit)
    }

    /**
     * @return the bundle needed for querying the LoaderManager for the offline list with the given id
     */
    public static Bundle getBundleForList(final Int listId) {
        val bundle: Bundle = Bundle()
        bundle.putInt(Intents.EXTRA_LIST_ID, listId)
        return bundle
    }

}
