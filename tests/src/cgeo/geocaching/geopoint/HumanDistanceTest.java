package cgeo.geocaching.geopoint;

import cgeo.geocaching.Settings;

import android.test.AndroidTestCase;

public class HumanDistanceTest extends AndroidTestCase {

    public static void testHumanDistance() {
        assertEquals("?", HumanDistance.getHumanDistance(null));
        if (Settings.isUseMetricUnits()) {
            assertEquals("123 km", HumanDistance.getHumanDistance(123.456f));
            assertEquals("123 m", HumanDistance.getHumanDistance(0.123456f));
        }
        else {
            assertEquals("77 mi", HumanDistance.getHumanDistance(123.456f));
        }
    }
}
