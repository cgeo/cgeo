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

package cgeo.geocaching.activity

/**
 * Interface to implement by activities that want to utilize the NavigationActionProvider
 */
interface INavigationSource {

    /**
     * Calls the default navigation in the current context
     */
    Unit startDefaultNavigation()

    /**
     * Calls the second default navigation in the current context
     */
    Unit startDefaultNavigation2()
}
