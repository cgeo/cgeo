package cgeo.geocaching.enumerations;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.loaders.AbstractSearchLoader.CacheListLoaderType;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, CacheListLoaderType.OFFLINE, GeocacheFilterContext.FilterType.OFFLINE, AbstractBottomNavigationActivity.MENU_LIST, true),
    POCKET(false, CacheListLoaderType.POCKET, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_HIDE_BOTTOM_NAVIGATION),
    HISTORY(true, CacheListLoaderType.HISTORY, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_LIST, true),
    NEAREST(false, CacheListLoaderType.NEAREST, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_NEARBY),
    COORDINATE(false, CacheListLoaderType.COORDINATE, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    KEYWORD(false, CacheListLoaderType.KEYWORD, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    ADDRESS(false, CacheListLoaderType.ADDRESS, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    FINDER(false, CacheListLoaderType.FINDER, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    OWNER(false, CacheListLoaderType.OWNER, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH),
    MAP(false, CacheListLoaderType.MAP, GeocacheFilterContext.FilterType.TRANSIENT, AbstractBottomNavigationActivity.MENU_MAP),
    SEARCH_FILTER(false, CacheListLoaderType.SEARCH_FILTER, GeocacheFilterContext.FilterType.LIVE, AbstractBottomNavigationActivity.MENU_SEARCH);

    /**
     * whether or not this list allows switching to another list
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

    @NonNull public final CacheListLoaderType loaderType;

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem) {
        this(canSwitch, loaderType, filterContextType, navigationMenuItem, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, @NonNull final GeocacheFilterContext.FilterType filterContextType, final int navigationMenuItem, final boolean isStoredInDatabase) {
        this.canSwitch = canSwitch;
        this.loaderType = loaderType;
        this.filterContextType = filterContextType;
        this.navigationMenuItem = navigationMenuItem;
        this.isStoredInDatabase = isStoredInDatabase;
    }

    public int getLoaderId() {
        return loaderType.getLoaderId();
    }

}
