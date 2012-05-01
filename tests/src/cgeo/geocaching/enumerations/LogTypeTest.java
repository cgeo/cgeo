package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class LogTypeTest extends AndroidTestCase {

    public static void testGetById() {
        assertEquals(LogType.UNKNOWN, LogType.getById(0));
        assertEquals(LogType.UNKNOWN, LogType.getById(4711));
        assertEquals(LogType.ENABLE_LISTING, LogType.getById(23));
    }

    public static void testGetByIconName() {
        assertEquals(LogType.UNKNOWN, LogType.getByIconName(""));
        assertEquals(LogType.UNKNOWN, LogType.getByIconName(null));
        assertEquals(LogType.WEBCAM_PHOTO_TAKEN, LogType.getByIconName("icon_camera"));
    }

    public static void testGetByType() {
        assertEquals(LogType.UNKNOWN, LogType.getByIconName(""));
        assertEquals(LogType.UNKNOWN, LogType.getByIconName(null));
        assertEquals(LogType.GRABBED_IT, LogType.getByType("grabbed IT "));
    }

}
