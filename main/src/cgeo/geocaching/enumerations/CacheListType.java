package cgeo.geocaching.enumerations;

import cgeo.geocaching.loaders.AbstractSearchLoader.CacheListLoaderType;

import androidx.annotation.NonNull;

public enum CacheListType {
    OFFLINE(true, CacheListLoaderType.OFFLINE, true),
    POCKET(false, CacheListLoaderType.POCKET),
    HISTORY(true, CacheListLoaderType.HISTORY, true),
    NEAREST(false, CacheListLoaderType.NEAREST),
    COORDINATE(false, CacheListLoaderType.COORDINATE),
    KEYWORD(false, CacheListLoaderType.KEYWORD),
    ADDRESS(false, CacheListLoaderType.ADDRESS),
    FINDER(false, CacheListLoaderType.FINDER),
    OWNER(false, CacheListLoaderType.OWNER),
    MAP(false, CacheListLoaderType.MAP);

    /**
     * whether or not this list allows switching to another list
     */
    public final boolean canSwitch;
    public final boolean isStoredInDatabase;

    @NonNull public final CacheListLoaderType loaderType;

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType) {
        this(canSwitch, loaderType, false);
    }

    CacheListType(final boolean canSwitch, @NonNull final CacheListLoaderType loaderType, final boolean isStoredInDatabase) {
        this.canSwitch = canSwitch;
        this.loaderType = loaderType;
        this.isStoredInDatabase = isStoredInDatabase;
    }

    public int getLoaderId() {
        return loaderType.getLoaderId();
    }

}
