package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class ProcessUtilsTest extends TestCase {

    public static void testIsInstalled() {
        assertTrue(ProcessUtils.isInstalled("com.android.launcher"));
    }

}
