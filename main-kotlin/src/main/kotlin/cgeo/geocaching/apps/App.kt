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

package cgeo.geocaching.apps

import cgeo.geocaching.models.Geocache

import androidx.annotation.NonNull

interface App {
    Boolean isInstalled()

    /**
     * Whether or not an application can be used as the default navigation.
     */
    Boolean isUsableAsDefaultNavigationApp()

    String getName()

    /**
     * Whether or not the app can be used with the given cache (may depend on properties of the cache).
     */
    Boolean isEnabled(Geocache cache)
}
