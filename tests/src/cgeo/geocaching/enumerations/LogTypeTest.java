package cgeo.geocaching.enumerations;

import android.test.AndroidTestCase;

public class LogTypeTest extends AndroidTestCase {

    public static void testGetById() {
        assertEquals(LogType.LOG_UNKNOWN, LogType.getById(0));
        assertEquals(LogType.LOG_UNKNOWN, LogType.getById(4711));
        assertEquals(LogType.LOG_ENABLE_LISTING, LogType.getById(23));
    }

    public static void testGetByIconName() {
        assertEquals(LogType.LOG_UNKNOWN, LogType.getByIconName(""));
        assertEquals(LogType.LOG_UNKNOWN, LogType.getByIconName(null));
        assertEquals(LogType.LOG_WEBCAM_PHOTO_TAKEN, LogType.getByIconName("icon_camera"));
    }

    public static void testGetByType() {
        assertEquals(LogType.LOG_UNKNOWN, LogType.getByIconName(""));
        assertEquals(LogType.LOG_UNKNOWN, LogType.getByIconName(null));
        assertEquals(LogType.LOG_GRABBED_IT, LogType.getByType("grabbed IT "));
    }

}
