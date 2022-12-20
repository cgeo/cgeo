package cgeo.geocaching.maps;

/**
 * Controls the behavior of the map
 */
public enum MapMode {
    /**
     * Live Map
     */
    LIVE,
    /**
     * Map around some coordinates
     */
    COORDS,
    /**
     * Map with a single cache (no reload on move)
     */
    SINGLE,
    /**
     * Map with a list of caches (no reload on move)
     */
    LIST
}
