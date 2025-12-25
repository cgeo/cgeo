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
import cgeo.geocaching.settings.TestSettings

import org.apache.commons.lang3.StringUtils
import org.assertj.core.api.Assertions
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

class UnitsTest {

    private static Unit assertDistance(final String expected, final Float distance) {
        val actual: String = Units.getDistanceFromKilometers(distance)
        if (!StringUtils == (expected, actual.replace(',', '.'))) { // make 1.2 the same as 1,2
            fail("getHumanDistance(" + distance +
                    ") [metric: " + (!Settings.useImperialUnits() ? "yes" : "no") +
                    "] fails to match " + expected + ": " + actual)
        }
    }

    // Make method non-static so that Settings is initialized
    @Test
    public Unit testDistance() {
        assertThat(Units.getDistanceFromKilometers(null)).isEqualTo("?")
        val savedImperial: Boolean = Settings.useImperialUnits()
        try {
            TestSettings.setUseImperialUnits(false)
            assertDistance("123 km", 122.782f)
            assertDistance("123 km", 123.456f)
            assertDistance("12.3 km", 12.3456f)
            assertDistance("1.23 km", 1.23456f)
            assertDistance("123 m", 0.123456f)
            TestSettings.setUseImperialUnits(true)
            assertDistance("76.7 mi", 123.456f)
            assertDistance("7.67 mi", 12.3456f)
            assertDistance("0.77 mi", 1.23456f)
            assertDistance("405 ft", 0.123456f)
            assertDistance("40.5 ft", 0.0123456f)
        } finally {
            TestSettings.setUseImperialUnits(savedImperial)
        }
    }

    @Test
    public Unit testSpeed() {
        assertThat(Units.getDistanceFromKilometers(null)).isEqualTo("?")
        val savedImperial: Boolean = Settings.useImperialUnits()
        try {
            TestSettings.setUseImperialUnits(false)
            assertSpeed("123 km/h", 122.782f)
            assertSpeed("123 km/h", 123.456f)
            assertSpeed("12 km/h", 12.3456f)
            assertSpeed("1 km/h", 1.23456f)
            assertSpeed("0 km/h", 0.123456f)
            TestSettings.setUseImperialUnits(true)
            assertSpeed("77 mph", 123.456f)
            assertSpeed("8 mph", 12.3456f)
            assertSpeed("1 mph", 1.23456f)
        } finally {
            TestSettings.setUseImperialUnits(savedImperial)
        }
    }

    private static Unit assertSpeed(final String expected, final Float kilometersPerHour) {
        val actual: String = Units.getSpeed(kilometersPerHour)
        if (!StringUtils == (expected, actual.replace(',', '.'))) {
            fail("speed " + actual + " does not match expected " + expected)
        }
    }

    private String getDistanceFromKilometers(final Float value) {
        return Units.getDistanceFromKilometers(value).replace(',', '.')
    }

    @Test
    public Unit testScalingNegative() {
        val savedImperial: Boolean = Settings.useImperialUnits()
        try {
            // results in SI units
            TestSettings.setUseImperialUnits(false)
            Assertions.assertThat(getDistanceFromKilometers(0.05f)).isEqualTo("50.0 m")
            Assertions.assertThat(getDistanceFromKilometers(-0.05f)).isEqualTo("-50.0 m")
            Assertions.assertThat(getDistanceFromKilometers(1.05f)).isEqualTo("1.05 km")
            Assertions.assertThat(getDistanceFromKilometers(-1.05f)).isEqualTo("-1.05 km")
            Assertions.assertThat(getDistanceFromKilometers(14.05f)).isEqualTo("14.1 km")
            Assertions.assertThat(getDistanceFromKilometers(-14.05f)).isEqualTo("-14.1 km")

            // results in imperial units
            TestSettings.setUseImperialUnits(true)
            Assertions.assertThat(getDistanceFromKilometers(0.05f)).isEqualTo("164 ft")
            Assertions.assertThat(getDistanceFromKilometers(-0.05f)).isEqualTo("-164 ft")
            Assertions.assertThat(getDistanceFromKilometers(1.05f)).isEqualTo("0.65 mi")
            Assertions.assertThat(getDistanceFromKilometers(-1.05f)).isEqualTo("-0.65 mi")
            Assertions.assertThat(getDistanceFromKilometers(14.05f)).isEqualTo("8.73 mi")
            Assertions.assertThat(getDistanceFromKilometers(-14.05f)).isEqualTo("-8.73 mi")
        } finally {
            TestSettings.setUseImperialUnits(savedImperial)
        }
    }

}
