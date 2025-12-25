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

package cgeo.geocaching.apps.navi

import cgeo.geocaching.apps.App
import cgeo.geocaching.models.Geocache

import android.content.Context

import androidx.annotation.NonNull

/**
 * interface for navigation to a cache
 */
interface CacheNavigationApp : App() {
    /**
     * Navigate to the given cache. The caller will assert that cache.getCoords() is not null.
     */
    Unit navigate(Context context, Geocache cache)
}
