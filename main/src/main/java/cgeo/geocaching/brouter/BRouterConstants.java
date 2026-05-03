package cgeo.geocaching.brouter;

public class BRouterConstants {
    // BRouter basis for c:geo's implementation
    public static final String version = "1.7.9";

    public static final String BROUTER_LOOKUPS_FILENAME = "lookups.dat";

    public static final String BROUTER_PROFILE_FILEEXTENSION = ".brf";
    public static final String BROUTER_PROFILE_WALK_DEFAULT = "shortest.brf";
    public static final String BROUTER_PROFILE_BIKE_DEFAULT = "trekking.brf";
    public static final String BROUTER_PROFILE_CAR_DEFAULT = "car-eco.brf";
    public static final String BROUTER_PROFILE_ELEVATION_ONLY = "dummy.brf";

    public static final String BROUTER_TILE_FILEEXTENSION = ".rd5";
    public static final String BROUTER_LOOKUPS_FILEEXTENSION = ".dat";

    public static final String PROFILE_PARAMTERKEY = "internal_routing_profile";

    private BRouterConstants() {
        // utility class
    }
}
