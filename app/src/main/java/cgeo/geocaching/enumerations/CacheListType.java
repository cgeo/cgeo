package cgeo.geocaching.enumerations;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.filters.core.GeocacheFilterContext;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, GeocacheFilterContext.FilterType.OFFLINE, AbstractBottomNavigationActivity.MENU_LIST, true, false),
    POCKET(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_HIDE_BOTTOM_NAVIGATION, false, true),
    HISTORY(true, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_LIST, true, false),
    NEAREST(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_NEARBY, false, true),
    COORDINATE(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true),
    KEYWORD(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true),
    ADDRESS(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true),
    FINDER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true),
    OWNER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true),
    MAP(false, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_HIDE_BOTTOM_NAVIGATION, false, false),
    SEARCH_FILTER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH, false, true);

    /**
     * whether or not this list allows switching to another list type
     */
    public final boolean canSwitch;
    /**
     * The filter context, which should be used for the list type.
     * E.g. LIVE or OFFLINE
     */
    public final GeocacheFilterContext.FilterType filterContextType;
    /**
     * The corresponding bottom navigation item which should be selected while the list is active
     */
    public final int navigationMenuItem;
    public final boolean isStoredInDatabase;
    public final boolean isOnline;

    CacheListType(final boolean canSwitch, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem) {
        this(canSwitch, filterContextType, navigationMenuItem, false, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem, final boolean isStoredInDatabase, final boolean isOnline) {
        this.canSwitch = canSwitch;
        this.filterContextType = filterContextType;
        this.navigationMenuItem = navigationMenuItem;
        this.isStoredInDatabase = isStoredInDatabase;
        this.isOnline = isOnline;
    }

}
