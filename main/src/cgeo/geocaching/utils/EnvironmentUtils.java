package cgeo.geocaching.utils;

import android.os.Environment;

public class EnvironmentUtils {
    private EnvironmentUtils() {
        // utility class
    }

    public static boolean isExternalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
}
