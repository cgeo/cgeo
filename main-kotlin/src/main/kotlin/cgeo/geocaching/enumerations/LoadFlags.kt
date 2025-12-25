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

import androidx.annotation.NonNull

import java.util.EnumSet

/**
 * Cache loading/saving/removing parameters
 */
interface LoadFlags {

    enum class LoadFlag {
        CACHE_BEFORE, // load from CacheCache
        CACHE_AFTER, // load from CacheCache
        DB_MINIMAL, // load minimal informations from DataBase
        ATTRIBUTES,
        WAYPOINTS,
        SPOILERS,
        LOGS,
        INVENTORY,
        OFFLINE_LOG,
        CATEGORIES
    }

    /**
     * Retrieve cache from CacheCache only. Do not load from DB
     */
    EnumSet<LoadFlag> LOAD_CACHE_ONLY = EnumSet.of(LoadFlag.CACHE_BEFORE)
    /**
     * Retrieve cache from CacheCache first. If not found load from DB
     */
    EnumSet<LoadFlag> LOAD_CACHE_OR_DB = EnumSet.of(LoadFlag.CACHE_BEFORE, LoadFlag.DB_MINIMAL, LoadFlag.OFFLINE_LOG)
    /**
     * Retrieve cache (minimalistic information including waypoints) from DB first. If not found load from CacheCache
     */
    EnumSet<LoadFlag> LOAD_WAYPOINTS = EnumSet.of(LoadFlag.CACHE_AFTER, LoadFlag.DB_MINIMAL, LoadFlag.WAYPOINTS, LoadFlag.OFFLINE_LOG)
    /**
     * Retrieve cache (all stored informations) from DB only. Do not load from CacheCache
     */
    EnumSet<LoadFlag> LOAD_ALL_DB_ONLY = EnumSet.range(LoadFlag.DB_MINIMAL, LoadFlag.OFFLINE_LOG)

    enum class SaveFlag {
        CACHE, // save only to CacheCache
        DB // include saving to CacheCache
    }

    EnumSet<SaveFlag> SAVE_ALL = EnumSet.allOf(SaveFlag.class)

    enum class RemoveFlag {
        CACHE, // save only to CacheCache
        DB, // includes removing from CacheCache
        OWN_WAYPOINTS_ONLY_FOR_TESTING // only to be used in unit testing (as we never delete own waypoints)
    }

    EnumSet<RemoveFlag> REMOVE_ALL = EnumSet.of(RemoveFlag.CACHE, RemoveFlag.DB)

}
