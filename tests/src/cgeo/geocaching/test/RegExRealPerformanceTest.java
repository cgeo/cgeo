package cgeo.geocaching.test;

import cgeo.geocaching.cgSettings;

import android.test.AndroidTestCase;
import android.util.Log;

import java.util.List;

/**
 * Test class to compare the performance of two regular expressions on given data.
 * Can be used to improve the time needed to parse the cache data
 *
 * @author blafoo
 */
public class RegExRealPerformanceTest extends AndroidTestCase {

    public void testRegEx() {

        List<String> output = RegExPerformanceTest.doTheTests();

        for (String s : output) {
            Log.w(cgSettings.tag, s);
        }

    }
}
