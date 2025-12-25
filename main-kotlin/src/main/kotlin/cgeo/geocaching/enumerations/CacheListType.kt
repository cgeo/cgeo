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

package cgeo.geocaching.enumerations

import cgeo.geocaching.activity.AbstractNavigationBarActivity
import cgeo.geocaching.filters.core.GeocacheFilterContext

import androidx.annotation.NonNull

enum class class CacheListType {
    OFFLINE(true, GeocacheFilterContext.FilterType.OFFLINE, AbstractNavigationBarActivity.MENU_LIST, true, false),
    POCKET(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_HIDE_NAVIGATIONBAR, false, true),
    HISTORY(true, GeocacheFilterContext.FilterType.TRANSIENT, AbstractNavigationBarActivity.MENU_LIST, true, false),
    NEAREST(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_CUSTOM, false, true),
    COORDINATE(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true),
    KEYWORD(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true),
    ADDRESS(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true),
    FINDER(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true),
    OWNER(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true),
    MAP(false, GeocacheFilterContext.FilterType.TRANSIENT, AbstractNavigationBarActivity.MENU_HIDE_NAVIGATIONBAR, false, false),
    LAST_VIEWED(false, GeocacheFilterContext.FilterType.TRANSIENT, AbstractNavigationBarActivity.MENU_SEARCH, false, false),
    SEARCH_FILTER(false, GeocacheFilterContext.FilterType.LIVE, AbstractNavigationBarActivity.MENU_SEARCH, false, true)

    /**
     * whether or not this list allows switching to another list type
     */
    public final Boolean canSwitch
    /**
     * The filter context, which should be used for the list type.
     * E.g. LIVE or OFFLINE
     */
    public final GeocacheFilterContext.FilterType filterContextType
    /**
     * The corresponding bottom navigation item which should be selected while the list is active
     */
    public final Int navigationMenuItem
    public final Boolean isStoredInDatabase
    public final Boolean isOnline

    CacheListType(final Boolean canSwitch, final GeocacheFilterContext.FilterType filterContextType, final Int navigationMenuItem, final Boolean isStoredInDatabase, final Boolean isOnline) {
        this.canSwitch = canSwitch
        this.filterContextType = filterContextType
        this.navigationMenuItem = navigationMenuItem
        this.isStoredInDatabase = isStoredInDatabase
        this.isOnline = isOnline
    }

}
