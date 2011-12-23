package cgeo.geocaching.enumerations;

import java.util.EnumSet;

/**
 * Cache loading parameters
 * 
 * @author blafoo
 */
public class LoadFlags {

    public enum LoadFlag {
        LOADATTRIBUTES,
        LOADWAYPOINTS,
        LOADSPOILERS,
        LOADLOGS,
        LOADINVENTORY,
        LOADOFFLINELOG
    }

    public final static EnumSet<LoadFlag> LOADALL = EnumSet.allOf(LoadFlag.class);

}