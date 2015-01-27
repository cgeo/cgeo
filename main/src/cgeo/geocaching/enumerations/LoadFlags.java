package cgeo.geocaching.enumerations;

import java.util.EnumSet;

/**
 * Cache loading/saving/removing parameters
 */
public interface LoadFlags {

    public enum LoadFlag {
        CACHE_BEFORE, // load from CacheCache
        CACHE_AFTER, // load from CacheCache
        DB_MINIMAL, // load minimal informations from DataBase
        ATTRIBUTES,
        WAYPOINTS,
        SPOILERS,
        LOGS,
        INVENTORY,
        OFFLINE_LOG
    }

    /** Retrieve cache from CacheCache only. Do not load from DB */
    public final static EnumSet<LoadFlag> LOAD_CACHE_ONLY = EnumSet.of(LoadFlag.CACHE_BEFORE);
    /** Retrieve cache from CacheCache first. If not found load from DB */
    public final static EnumSet<LoadFlag> LOAD_CACHE_OR_DB = EnumSet.of(LoadFlag.CACHE_BEFORE, LoadFlag.DB_MINIMAL, LoadFlag.OFFLINE_LOG, LoadFlag.SPOILERS);
    /** Retrieve cache (minimalistic information including waypoints) from DB first. If not found load from CacheCache */
    public final static EnumSet<LoadFlag> LOAD_WAYPOINTS = EnumSet.of(LoadFlag.CACHE_AFTER, LoadFlag.DB_MINIMAL, LoadFlag.WAYPOINTS, LoadFlag.OFFLINE_LOG, LoadFlag.SPOILERS);
    /** Retrieve cache (all stored informations) from DB only. Do not load from CacheCache */
    public final static EnumSet<LoadFlag> LOAD_ALL_DB_ONLY = EnumSet.range(LoadFlag.DB_MINIMAL, LoadFlag.OFFLINE_LOG);

    public enum SaveFlag {
        CACHE, // save only to CacheCache
        DB // include saving to CacheCache
    }

    public final static EnumSet<SaveFlag> SAVE_ALL = EnumSet.allOf(SaveFlag.class);

    public enum RemoveFlag {
        CACHE, // save only to CacheCache
        DB, // includes removing from CacheCache
        OWN_WAYPOINTS_ONLY_FOR_TESTING // only to be used in unit testing (as we never delete own waypoints)
    }

    public final static EnumSet<RemoveFlag> REMOVE_ALL = EnumSet.of(RemoveFlag.CACHE, RemoveFlag.DB);

}