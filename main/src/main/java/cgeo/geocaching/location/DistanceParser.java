package cgeo.geocaching.location;

public final class DistanceParser {

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
    public static float convertDistance(final float distance, final DistanceUnit unit) {
        switch (unit) {
            case METER:
                return distance / 1000;
            case FEET:
                return distance * IConversion.FEET_TO_KILOMETER;
            case MILE:
                return distance * IConversion.MILES_TO_KILOMETER;
            case YARD:
                return distance * IConversion.YARDS_TO_KILOMETER;
            case KILOMETER:
            default:
                return distance;
        }
    }

}
