package cgeo.geocaching.enumerations;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;

public class LogTypeTrackableTest extends AndroidTestCase {

    public static void testFindById() {
        for (LogTypeTrackable logTypeTrackable : LogTypeTrackable.values()) {
            assertTrue(StringUtils.isNotEmpty(logTypeTrackable.getLabel()));
        }
    }

}
