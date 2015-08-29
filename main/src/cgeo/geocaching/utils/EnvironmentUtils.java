package cgeo.geocaching.utils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.os.Environment;

public class EnvironmentUtils {
    private EnvironmentUtils() {
        // utility class
    }

    /**
     * Same as {@link Environment#getExternalStorageState()} but more stable. We have seen null pointers here, probably
     * when there are issues in the underlying mount.
     */
    @NonNull
    public static String getExternalStorageState() {
        try {
            return Environment.getExternalStorageState();
        } catch (final NullPointerException e) {
            Log.w("Could not get external storage state", e);
        }
        return StringUtils.EMPTY;
    }

    public static boolean isExternalStorageAvailable() {
        return EnvironmentUtils.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
