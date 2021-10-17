package cgeo.geocaching.enumerations;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.filters.core.GeocacheFilterContext;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, GeocacheFilterContext.FilterType.OFFLINE, AbstractBottomNavigationActivity.MENU_LIST, true),
    POCKET(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_HIDE_BOTTOM_NAVIGATION),
    HISTORY(true, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_LIST, true),
    NEAREST(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_NEARBY),
    COORDINATE(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    KEYWORD(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    ADDRESS(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    FINDER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    OWNER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    MAP(false, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_MAP),
    SEARCH_FILTER(false, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH);

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

    CacheListType(final boolean canSwitch, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem) {
        this(canSwitch, filterContextType, navigationMenuItem, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem, final boolean isStoredInDatabase) {
        this.canSwitch = canSwitch;
        this.filterContextType = filterContextType;
        this.navigationMenuItem = navigationMenuItem;
        this.isStoredInDatabase = isStoredInDatabase;
    }

}
