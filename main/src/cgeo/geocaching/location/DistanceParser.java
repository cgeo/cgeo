package cgeo.geocaching.location;

import java.util.Locale;

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
            case M:
                return distance / 1000;
            case FT:
                return distance * IConversion.FEET_TO_KILOMETER;
            case MI:
                return distance * IConversion.MILES_TO_KILOMETER;
            case YD:
                return distance * IConversion.YARDS_TO_KILOMETER;
            case KM:
            default:
                return distance;
        }
    }

    public enum DistanceUnit {
        M(0), KM(1), FT(2), YD(3), MI(4);
        private final int value;

        DistanceUnit(final int value) {
            this.value = value;
        }

        public static DistanceUnit getById(final int id) {
            for (final DistanceUnit e : values()) {
                if (e.value == id) {
                    return e;
                }
            }
            return MI;
        }

        /**
         * Parses English-language string representing {@link DistanceUnit} object
         *
         * @param unitStr string like "km" to parse
         * @return DistanceUnit object corresponding to the string
         * @throws NumberFormatException if string is empty or cannot be parsed to {@link DistanceUnit}
         */
        public static DistanceUnit parseUnit(final String unitStr) throws NumberFormatException {
            final DistanceUnit unit;

            switch (unitStr.toLowerCase(Locale.getDefault())) {
                case "km":
                    unit = DistanceUnit.KM;
                    break;
                case "mi":
                    unit = DistanceUnit.MI;
                    break;
                case "m":
                    unit = DistanceUnit.M;
                    break;
                case "ft":
                    unit = DistanceUnit.FT;
                    break;
                case "yd":
                    unit = DistanceUnit.YD;
                    break;
                default:
                    throw new NumberFormatException();
            }
            return unit;
        }

        /**
         * Parses English-language string {@code unitStr} representing {@link DistanceUnit} object.
         * If the string cannot be parsed to a valid unit, {@code defaultUnit} will be returned
         *
         * @param unitStr     string like "km" to parse
         * @param defaultUnit unit which will be returned if parsing fails
         * @return DistanceUnit object corresponding to the string, or {@code defaultUnit}
         * if the string cannot be parsed
         */
        public static DistanceUnit parseUnit(final String unitStr, final DistanceUnit defaultUnit) {
            try {
                return parseUnit(unitStr);
            } catch (final NumberFormatException e) {
                return defaultUnit;
            }
        }

        public int getValue() {
            return value;
        }
    }

}
