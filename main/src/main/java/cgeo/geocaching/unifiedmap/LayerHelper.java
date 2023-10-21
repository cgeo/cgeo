package cgeo.geocaching.unifiedmap;

public class LayerHelper {

    // constants for layer ordering (higher = more visible)
    // layer index numbers must be consecutive, starting with 1 for ZINDEX_BASEMAP

    public static final int ZINDEX_POSITION = 14;

    public static final int ZINDEX_SEARCHCENTER = 13;

    public static final int ZINDEX_TRACK_ROUTE = 12;

    public static final int ZINDEX_DIRECTION_LINE = 11;

    public static final int ZINDEX_GEOCACHE = 10;
    public static final int ZINDEX_WAYPOINT = 9;
    public static final int ZINDEX_COORD_POINT = 8;

    public static final int ZINDEX_POSITION_ACCURACY_CIRCLE = 7;

    public static final int ZINDEX_HISTORY = 6;

    public static final int ZINDEX_CIRCLE = 5;

    // some OSM-only layers
    public static final int ZINDEX_LABELS = 4;
    public static final int ZINDEX_BUILDINGS = 3;
    public static final int ZINDEX_SCALEBAR = 2;
    public static final int ZINDEX_BASEMAP = 1;     // this is hard-coded in VTM:Map.java

    private LayerHelper() {
        // helper class
    }

}
