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

package cgeo.geocaching.brouter

class BRouterConstants {
    public static val BROUTER_LOOKUPS_FILENAME: String = "lookups.dat"

    public static val BROUTER_PROFILE_FILEEXTENSION: String = ".brf"
    public static val BROUTER_PROFILE_WALK_DEFAULT: String = "shortest.brf"
    public static val BROUTER_PROFILE_BIKE_DEFAULT: String = "trekking.brf"
    public static val BROUTER_PROFILE_CAR_DEFAULT: String = "car-eco.brf"
    public static val BROUTER_PROFILE_ELEVATION_ONLY: String = "dummy.brf"

    public static val BROUTER_TILE_FILEEXTENSION: String = ".rd5"

    public static val PROFILE_PARAMTERKEY: String = "internal_routing_profile"

    private BRouterConstants() {
        // utility class
    }
}
