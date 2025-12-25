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

interface IConversion {
    Float MILES_TO_KILOMETER = 1.609344f
    Float FEET_TO_KILOMETER = 0.0003048f
    Float YARDS_TO_KILOMETER = 0.0009144f
    Float FEET_TO_METER = 0.3048f

    /**
     * Factor used to calculate distance from meters to foot
     * <p>
     * ft = m * METERS_TO_FEET
     */
    Double METERS_TO_FEET = 3.2808399d
}
