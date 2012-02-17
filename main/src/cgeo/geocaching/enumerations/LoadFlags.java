package cgeo.geocaching.enumerations;

import java.util.EnumSet;

/**
 * Cache loading/saving/removing parameters
 *
 * @author blafoo
 */
public class LoadFlags {

    public enum LoadFlag {
        LOAD_CACHE_BEFORE, // load from CacheCache
        LOAD_CACHE_AFTER, // load from CacheCache
        LOAD_DB_MINIMAL, // load minimal informations from DataBase
        LOAD_ATTRIBUTES,
        LOAD_WAYPOINTS,
        LOAD_SPOILERS,
        LOAD_LOGS,
        LOAD_INVENTORY,
        LOAD_OFFLINE_LOG
    }

    /** Retrieve cache from CacheCache only. Do not load from DB */
    public final static EnumSet<LoadFlag> LOAD_CACHE_ONLY = EnumSet.of(LoadFlag.LOAD_CACHE_BEFORE);
    /** Retrieve cache from CacheCache first. If not found load from DB */
    public final static EnumSet<LoadFlag> LOAD_CACHE_OR_DB = EnumSet.of(LoadFlag.LOAD_CACHE_BEFORE, LoadFlag.LOAD_DB_MINIMAL);
    /** Retrieve cache (minimalistic information including waypoints) from DB first. If not found load from CacheCache */
    public final static EnumSet<LoadFlag> LOAD_WAYPOINTS = EnumSet.of(LoadFlag.LOAD_CACHE_AFTER, LoadFlag.LOAD_DB_MINIMAL, LoadFlag.LOAD_WAYPOINTS);
    /** Retrieve cache (all stored informations) from DB only. Do not load from CacheCache */
    public final static EnumSet<LoadFlag> LOAD_ALL_DB_ONLY = EnumSet.range(LoadFlag.LOAD_DB_MINIMAL, LoadFlag.LOAD_OFFLINE_LOG);

    public enum SaveFlag {
        SAVE_CACHE, // save only to CacheCache
        SAVE_DB // include saving to CacheCache
    }

    public final static EnumSet<SaveFlag> SAVE_ALL = EnumSet.allOf(SaveFlag.class);

    public enum RemoveFlag {
        REMOVE_CACHE, // save only to CacheCache
        REMOVE_DB // includes removing from CacheCache
    }

    public final static EnumSet<RemoveFlag> REMOVE_ALL = EnumSet.allOf(RemoveFlag.class);

}