package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class LogTypeTrackableTest extends AndroidTestCase {

    public static void testFindById() {
        assertEquals(LogTypeTrackable.DO_NOTHING, LogTypeTrackable.findById(12345));
    }

}
