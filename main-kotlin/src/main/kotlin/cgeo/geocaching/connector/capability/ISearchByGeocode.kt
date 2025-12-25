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

package cgeo.geocaching.connector.capability

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.utils.DisposableHandler

import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

/**
 * connector capability of searching online for a cache by geocode
 */
interface ISearchByGeocode : IConnector() {
    @WorkerThread
    SearchResult searchByGeocode(String geocode, String guid, DisposableHandler handler)
}
