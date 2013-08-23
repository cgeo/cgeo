package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class ProcessUtilsTest extends TestCase {

    public static void testIsInstalled() {
        assertTrue(ProcessUtils.isInstalled("com.android.launcher"));
    }

    public static void testIsInstalledNotLaunchable() {
        final String packageName = "com.android.systemui";
        assertTrue(ProcessUtils.isInstalled(packageName));
        assertFalse(ProcessUtils.isLaunchable(packageName));
    }

    public static void testIsLaunchable() {
        assertTrue(ProcessUtils.isInstalled("com.android.settings"));
    }

}
