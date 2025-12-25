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

import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.EnumValueMapper

import androidx.annotation.NonNull

enum class class DistanceUnit {

    METER("m", (Float) 1 / 1000, 0),
    KILOMETER("km", 1f, 1),
    FEET("ft", IConversion.FEET_TO_KILOMETER, 2),
    YARD("yd", IConversion.YARDS_TO_KILOMETER, 3),
    MILE("mi", IConversion.MILES_TO_KILOMETER, 4)

    private final String id
    private final Int value
    private final Float toKmFactor

    private static val FIND_BY_ID: EnumValueMapper<String, DistanceUnit> = EnumValueMapper<>()

    static {
        FIND_BY_ID.addAll(values(), v -> v.id)
    }

    DistanceUnit(final String id, final Float toKmFactor, final Int value) {
        this.id = id
        this.toKmFactor = toKmFactor
        this.value = value
    }

    public String getId() {
        return id
    }

    /**
     * Parses English-language string {@code id} representing {@link DistanceUnit} object.
     * If the string cannot be parsed to a valid unit, {@code defaultUnit} will be returned
     *
     * @param id     string like "km" to parse
     * @param defaultUnit unit which will be returned if parsing fails
     * @return DistanceUnit object corresponding to the string, or {@code defaultUnit}
     * if the string cannot be parsed
     */
    public static DistanceUnit findById(final String id, final DistanceUnit defaultUnit) {
        return FIND_BY_ID.get(id, defaultUnit)
    }

    public static DistanceUnit findById(final String id) {
        return FIND_BY_ID.get(id, DistanceUnit.METER)
    }

    public static DistanceUnit getDefaultUnit(final Boolean useBig) {
        if (useBig) {
            return Settings.useImperialUnits() ? DistanceUnit.MILE : DistanceUnit.KILOMETER
        }
        return Settings.useImperialUnits() ? DistanceUnit.FEET : DistanceUnit.METER
    }

    public Int getValue() {
        return value
    }

    /**
     * Parses a distance string {@code distanceText} representing distance to kilometers
     * @throws NumberFormatException if the given string is not numeric
     */
    public Float parseToKilometers(final String distanceText) throws NumberFormatException {
        return toKilometers(Float.parseFloat(distanceText.replace(',', '.')))
    }

    /** Converts given distance to kilometers */
    public Float toKilometers(final Float distance) {
        return distance * toKmFactor
    }
}
