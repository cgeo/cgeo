package cgeo.geocaching.enumerations;

import java.util.EnumSet;

/**
 * Cache loading/saving/removing parameters
 *
 * @author blafoo
 */
public class LoadFlags {

    public enum LoadFlag {
        LOADCACHEONLY, // load only from CacheCache
        LOADDBMINIMAL, // minimal informations from DataBase
        LOADATTRIBUTES,
        LOADWAYPOINTS,
        LOADSPOILERS,
        LOADLOGS,
        LOADINVENTORY,
        LOADOFFLINELOG
    }

    public final static EnumSet<LoadFlag> LOADCACHEONLY = EnumSet.of(LoadFlag.LOADCACHEONLY);
    public final static EnumSet<LoadFlag> LOADDBMINIMAL = EnumSet.of(LoadFlag.LOADCACHEONLY, LoadFlag.LOADDBMINIMAL);
    public final static EnumSet<LoadFlag> LOADWAYPOINTS = EnumSet.of(LoadFlag.LOADDBMINIMAL, LoadFlag.LOADWAYPOINTS);
    public final static EnumSet<LoadFlag> LOADALL = EnumSet.range(LoadFlag.LOADATTRIBUTES, LoadFlag.LOADOFFLINELOG);

    public enum SaveFlag {
        SAVECACHEONLY, // save only to CacheCache
        SAVEDB // include saving to CacheCache
    }

    public enum RemoveFlag {
        REMOVECACHEONLY, // save only to CacheCache
        REMOVEDB // includes removing from CacheCache
    }

}