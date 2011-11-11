package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class LogTypeTrackableTest extends AndroidTestCase {

    public void testFindById() {
        assertEquals(LogTypeTrackable.DO_NOTHING, LogTypeTrackable.findById(12345));
    }

}
