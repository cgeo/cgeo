package cgeo.geocaching.enumerations;

import java.util.EnumSet;

/**
 * Cache loading/saving/removing parameters
 *
 * @author blafoo
 */
public class LoadFlags {

    public enum LoadFlag {
        LOADCACHEBEFORE, // load from CacheCache
        LOADCACHEAFTER, // load from CacheCache
        LOADDBMINIMAL, // load minimal informations from DataBase
        LOADATTRIBUTES,
        LOADWAYPOINTS,
        LOADSPOILERS,
        LOADLOGS,
        LOADINVENTORY,
        LOADOFFLINELOG
    }

    /** Retrieve cache from CacheCache only. Do not load from DB */
    public final static EnumSet<LoadFlag> LOADCACHEONLY = EnumSet.of(LoadFlag.LOADCACHEBEFORE);
    /** Retrieve cache from CacheCache first. If not found load from DB */
    public final static EnumSet<LoadFlag> LOADCACHEORDB = EnumSet.of(LoadFlag.LOADCACHEBEFORE, LoadFlag.LOADDBMINIMAL);
    /** Retrieve cache (minimalistic informations including waypoints) from DB first. If not found load from CacheCache */
    public final static EnumSet<LoadFlag> LOADWAYPOINTS = EnumSet.of(LoadFlag.LOADCACHEAFTER, LoadFlag.LOADDBMINIMAL, LoadFlag.LOADWAYPOINTS);
    /** Retrieve cache (all stored informations) from DB only. Do not load from CacheCache */
    public final static EnumSet<LoadFlag> LOADALLDBONLY = EnumSet.range(LoadFlag.LOADDBMINIMAL, LoadFlag.LOADOFFLINELOG);

    public enum SaveFlag {
        SAVECACHE, // save only to CacheCache
        SAVEDB // include saving to CacheCache
    }

    public final static EnumSet<SaveFlag> SAVEALL = EnumSet.allOf(SaveFlag.class);

    public enum RemoveFlag {
        REMOVECACHE, // save only to CacheCache
        REMOVEDB // includes removing from CacheCache
    }

    public final static EnumSet<RemoveFlag> REMOVEALL = EnumSet.allOf(RemoveFlag.class);

}