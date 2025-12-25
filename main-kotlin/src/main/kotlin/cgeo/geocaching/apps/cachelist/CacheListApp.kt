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

package cgeo.geocaching.apps.cachelist

import cgeo.geocaching.SearchResult
import cgeo.geocaching.apps.App
import cgeo.geocaching.models.Geocache

import android.app.Activity

import androidx.annotation.NonNull

import java.util.List

interface CacheListApp : App() {

    Unit invoke(List<Geocache> caches,
                Activity activity, SearchResult search)

}
