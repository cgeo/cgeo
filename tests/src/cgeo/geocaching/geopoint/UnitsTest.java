package cgeo.geocaching.geopoint;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Settings;

import org.apache.commons.lang3.StringUtils;

public class UnitsTest extends CGeoTestCase {

    private static void assertDistance(final String expected, final float distance) {
        final String actual = Units.getDistanceFromKilometers(distance);
        if (!StringUtils.equals(expected, actual.replace(',', '.'))) { // make 1.2 the same as 1,2
            fail("getHumanDistance(" + distance +
                    ") [metric: " + (Settings.isUseMetricUnits() ? "yes" : "no") +
                    "] fails to match " + expected + ": " + actual);
        }
    }

    // Make method non-static so that Settings is initialized
    @SuppressWarnings("static-method")
    public void testDistance() {
        assertEquals("?", Units.getDistanceFromKilometers(null));
        final boolean savedMetrics = Settings.isUseMetricUnits();
        try {
            Settings.setUseMetricUnits(true);
            assertDistance("123 km", 122.782f);
            assertDistance("123 km", 123.456f);
            assertDistance("12.3 km", 12.3456f);
            assertDistance("1.23 km", 1.23456f);
            assertDistance("123 m", 0.123456f);
            Settings.setUseMetricUnits(false);
            assertDistance("76.7 mi", 123.456f);
            assertDistance("7.67 mi", 12.3456f);
            assertDistance("0.77 mi", 1.23456f);
            assertDistance("405 ft", 0.123456f);
            assertDistance("40.5 ft", 0.0123456f);
        } finally {
            Settings.setUseMetricUnits(savedMetrics);
        }
    }

    // Make method non-static so that Settings is initialized
    @SuppressWarnings("static-method")
    public void testElevation() {
        final boolean savedMetrics = Settings.isUseMetricUnits();
        try {
            Settings.setUseMetricUnits(true);
            assertElevation("↥ 123 m", 122.782f);
            assertElevation("↥ 123 m", 123.456f);
            assertElevation("↥ 12 m", 12.3456f);
            assertElevation("↥ 1 m", 1.23456f);
            assertElevation("↥ 2 m", 1.6f);
            assertElevation("↥ 0 m", 0.123456f);
            assertElevation("↧ 123 m", -122.782f);
            assertElevation("↧ 123 m", -123.456f);
            assertElevation("↧ 12 m", -12.3456f);
            assertElevation("↧ 1 m", -1.23456f);
            assertElevation("↧ 2 m", -1.6f);
            assertElevation("↧ 0 m", -0.123456f);
            Settings.setUseMetricUnits(false);
            assertElevation("↥ 405 ft", 123.456f);
            assertElevation("↥ 41 ft", 12.3456f);
        } finally {
            Settings.setUseMetricUnits(savedMetrics);
        }
    }

    private static void assertElevation(final String expected, final float meters) {
        final String actual = Units.getElevation(meters);
        if (!StringUtils.equals(expected, actual.replace(',', '.'))) {
            fail("elevation " + actual + " does not match expected " + expected);
        }
    }

    // Make method non-static so that Settings is initialized
    @SuppressWarnings("static-method")
    public void testSpeed() {
        assertEquals("?", Units.getDistanceFromKilometers(null));
        final boolean savedMetrics = Settings.isUseMetricUnits();
        try {
            Settings.setUseMetricUnits(true);
            assertSpeed("123 km/h", 122.782f);
            assertSpeed("123 km/h", 123.456f);
            assertSpeed("12.3 km/h", 12.3456f);
            assertSpeed("1.23 km/h", 1.23456f);
            assertSpeed("123 m/h", 0.123456f);
            Settings.setUseMetricUnits(false);
            assertSpeed("76.7 mph", 123.456f);
            assertSpeed("7.67 mph", 12.3456f);
            assertSpeed("0.77 mph", 1.23456f);
        } finally {
            Settings.setUseMetricUnits(savedMetrics);
        }
    }

    private static void assertSpeed(final String expected, final float kilometersPerHour) {
        final String actual = Units.getSpeed(kilometersPerHour);
        if (!StringUtils.equals(expected, actual.replace(',', '.'))) {
            fail("speed " + actual + " does not match expected " + expected);
        }
    }

}
