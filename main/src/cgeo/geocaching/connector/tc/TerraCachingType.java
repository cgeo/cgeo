package cgeo.geocaching.connector.tc;

import cgeo.geocaching.enumerations.CacheType;

/**
 * Adapter for cache types used on TerraCaching
 */
public final class TerraCachingType {

    private TerraCachingType() {
        // utility class
    }

    public static final CacheType getCacheType(final String style) {
        switch (style) {
            case "Classic":
                return CacheType.TRADITIONAL;
            case "Virtual":
                return CacheType.VIRTUAL;
            case "Puzzle":
                return CacheType.MYSTERY;
            case "Offset":
                return CacheType.MULTI;
            case "Event":
                return CacheType.EVENT;
        }
        return CacheType.UNKNOWN;
    }
}
