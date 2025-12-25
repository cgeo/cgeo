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

package cgeo.geocaching.unifiedmap

class LayerHelper {

    // constants for layer ordering (higher = more visible)
    // layer index numbers must be consecutive, starting with 1 for ZINDEX_BASEMAP

    public static val ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_GEOITEM: Int = 101
    public static val ZINDEX_CACHE_WAYPOINT_HIGHLIGHTER_MARKER: Int = 100

    public static val ZINDEX_ELEVATIONCHARTMARKERPOSITION: Int = 16
    public static val ZINDEX_POSITION: Int = 15
    public static val ZINDEX_POSITION_ELEVATION: Int = 14
    public static val ZINDEX_SEARCHCENTER: Int = 13
    public static val ZINDEX_SCALEBAR: Int = 12;   // OSM only
    public static val ZINDEX_GEOCACHE: Int = 11
    public static val ZINDEX_WAYPOINT: Int = 10
    public static val ZINDEX_COORD_POINT: Int = 9
    public static val ZINDEX_DIRECTION_LINE: Int = 8
    public static val ZINDEX_TRACK_ROUTE: Int = 7
    public static val ZINDEX_POSITION_ACCURACY_CIRCLE: Int = 6
    public static val ZINDEX_HISTORY: Int = 5
    public static val ZINDEX_CIRCLE: Int = 4
    public static val ZINDEX_LABELS: Int = 3;      // OSM only
    public static val ZINDEX_BUILDINGS: Int = 2;   // OSM only
    public static val ZINDEX_BASEMAP: Int = 1;     // this is hard-coded in VTM:Map.java

    private LayerHelper() {
        // helper class
    }

}
