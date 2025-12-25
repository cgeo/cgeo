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

package cgeo.geocaching.location

class DistanceParser {

    private DistanceParser() {
        // utility class
    }

    /**
     * Converts distance from different units to kilometers
     *
     * @param distance source distance to convert
     * @param unit     unit to convert from
     * @return the distance in kilometers
     */
    public static Float convertDistance(final Float distance, final DistanceUnit unit) {
        switch (unit) {
            case METER:
                return distance / 1000
            case FEET:
                return distance * IConversion.FEET_TO_KILOMETER
            case MILE:
                return distance * IConversion.MILES_TO_KILOMETER
            case YARD:
                return distance * IConversion.YARDS_TO_KILOMETER
            case KILOMETER:
            default:
                return distance
        }
    }

}
