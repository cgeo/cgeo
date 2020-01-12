package cgeo.geocaching.location;

import cgeo.CGeoTestCase;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UnitsTest extends CGeoTestCase {

    private static void assertDistance(final String expected, final float distance) {
        final String actual = Units.getDistanceFromKilometers(distance);
        if (!StringUtils.equals(expected, actual.replace(',', '.'))) { // make 1.2 the same as 1,2
            fail("getHumanDistance(" + distance +
                    ") [metric: " + (!Settings.useImperialUnits() ? "yes" : "no") +
                    "] fails to match " + expected + ": " + actual);
        }
    }

    // Make method non-static so that Settings is initialized
    public void testDistance() {
        assertThat(Units.getDistanceFromKilometers(null)).isEqualTo("?");
        final boolean savedImperial = Settings.useImperialUnits();
        try {
            TestSettings.setUseImperialUnits(false);
            assertDistance("123 km", 122.782f);
            assertDistance("123 km", 123.456f);
            assertDistance("12.3 km", 12.3456f);
            assertDistance("1.23 km", 1.23456f);
            assertDistance("123 m", 0.123456f);
            TestSettings.setUseImperialUnits(true);
            assertDistance("76.7 mi", 123.456f);
            assertDistance("7.67 mi", 12.3456f);
            assertDistance("0.77 mi", 1.23456f);
            assertDistance("405 ft", 0.123456f);
            assertDistance("40.5 ft", 0.0123456f);
        } finally {
            TestSettings.setUseImperialUnits(savedImperial);
        }
    }

    public void testSpeed() {
        assertThat(Units.getDistanceFromKilometers(null)).isEqualTo("?");
        final boolean savedImperial = Settings.useImperialUnits();
        try {
            TestSettings.setUseImperialUnits(false);
            assertSpeed("123 km/h", 122.782f);
            assertSpeed("123 km/h", 123.456f);
            assertSpeed("12 km/h", 12.3456f);
            assertSpeed("1 km/h", 1.23456f);
            assertSpeed("0 km/h", 0.123456f);
            TestSettings.setUseImperialUnits(true);
            assertSpeed("77 mph", 123.456f);
            assertSpeed("8 mph", 12.3456f);
            assertSpeed("1 mph", 1.23456f);
        } finally {
            TestSettings.setUseImperialUnits(savedImperial);
        }
    }

    private static void assertSpeed(final String expected, final float kilometersPerHour) {
        final String actual = Units.getSpeed(kilometersPerHour);
        if (!StringUtils.equals(expected, actual.replace(',', '.'))) {
            fail("speed " + actual + " does not match expected " + expected);
        }
    }

}
