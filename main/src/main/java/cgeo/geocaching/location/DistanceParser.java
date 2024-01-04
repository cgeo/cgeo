package cgeo.geocaching.location;

public final class DistanceParser {

    private DistanceParser() {
        // utility class
    }

    /**
     * Parses a distance string {@code distanceText} representing distance in units {@code unit}
     *
     * @param distanceText distance string
     * @param unit         unit to convert from
     * @return the distance in kilometers
     * @throws NumberFormatException if the given string is not numeric
     */
    public static float parseDistance(final String distanceText, final DistanceUnit unit)
            throws NumberFormatException {
        return convertDistance(Float.parseFloat(distanceText.replace(',', '.')), unit);
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
