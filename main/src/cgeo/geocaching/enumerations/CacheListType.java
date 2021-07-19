package cgeo.geocaching.enumerations;

import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.loaders.AbstractSearchLoader.CacheListLoaderType;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, CacheListLoaderType.OFFLINE, GeocacheFilterContext.FilterType.OFFLINE, true),
    POCKET(false, CacheListLoaderType.POCKET, GeocacheFilterContext.FilterType.LIVE),
    HISTORY(true, CacheListLoaderType.HISTORY, GeocacheFilterContext.FilterType.TRANSIENT, true),
    NEAREST(false, CacheListLoaderType.NEAREST, GeocacheFilterContext.FilterType.LIVE),
    COORDINATE(false, CacheListLoaderType.COORDINATE, GeocacheFilterContext.FilterType.LIVE),
    KEYWORD(false, CacheListLoaderType.KEYWORD, GeocacheFilterContext.FilterType.LIVE),
    ADDRESS(false, CacheListLoaderType.ADDRESS, GeocacheFilterContext.FilterType.LIVE),
    FINDER(false, CacheListLoaderType.FINDER, GeocacheFilterContext.FilterType.LIVE),
    OWNER(false, CacheListLoaderType.OWNER, GeocacheFilterContext.FilterType.LIVE),
    MAP(false, CacheListLoaderType.MAP, GeocacheFilterContext.FilterType.TRANSIENT),
    SEARCH_FILTER(false, CacheListLoaderType.SEARCH_FILTER, GeocacheFilterContext.FilterType.LIVE);

    /**
     * whether or not this list allows switching to another list
     */
    public final boolean canSwitch;
    public final GeocacheFilterContext.FilterType filterContextType;
    public final boolean isStoredInDatabase;

    @NonNull public final CacheListLoaderType loaderType;

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, @NonNull final GeocacheFilterContext.FilterType filterContextType) {
        this(canSwitch, loaderType, filterContextType, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, @NonNull final GeocacheFilterContext.FilterType filterContextType, final boolean isStoredInDatabase) {
        this.canSwitch = canSwitch;
        this.loaderType = loaderType;
        this.isStoredInDatabase = isStoredInDatabase;
        this.filterContextType = filterContextType;
    }

    public int getLoaderId() {
        return loaderType.getLoaderId();
    }

}
