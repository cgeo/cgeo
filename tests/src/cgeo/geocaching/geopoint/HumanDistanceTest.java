package cgeo.geocaching.geopoint;

import cgeo.geocaching.Settings;

import android.test.AndroidTestCase;

import java.util.regex.Pattern;

public class HumanDistanceTest extends AndroidTestCase {

    private static void assertMatch(final String ok, final float distance) {
        final String humanDistance = HumanDistance.getHumanDistance(distance);
        if (!Pattern.compile('^' + ok + '$').matcher(humanDistance).find()) {
            fail("getHumanDistance(" + distance +
                    ") [metric: " + (Settings.isUseMetricUnits() ? "yes" : "no") +
                    "] fails to match " + ok + ": " + humanDistance);
        }
    }

    // Make method non-static so that Settings is initialized
    @SuppressWarnings("static-method")
    public void testHumanDistance() {
        assertEquals("?", HumanDistance.getHumanDistance(null));
        final boolean savedMetrics = Settings.isUseMetricUnits();
        try {
            Settings.setUseMetricUnits(true);
            assertMatch("123 km", 122.782f);
            assertMatch("123 km", 123.456f);
            assertMatch("12.3 km", 12.3456f);
            assertMatch("1.23 km", 1.23456f);
            assertMatch("123 m", 0.123456f);
            Settings.setUseMetricUnits(false);
            assertMatch("76.7 mi", 123.456f);
            assertMatch("7.67 mi", 12.3456f);
            assertMatch("0.77 mi", 1.23456f);
            assertMatch("405 ft", 0.123456f);
            assertMatch("40.5 ft", 0.0123456f);
        } finally {
            Settings.setUseMetricUnits(savedMetrics);
        }
    }
}
