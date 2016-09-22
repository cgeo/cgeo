package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.res.Resources;
import android.support.annotation.RawRes;

import java.io.InputStream;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;

/**
 * Utility class for Android {@link Resources}.
 */
public final class ResourcesUtils {

    private ResourcesUtils() {
        // utility class
    }

    public static String getRawResourceString(@RawRes final int resourceId) {
        InputStream ins = null;
        Scanner scanner = null;
        try {
            ins = CgeoApplication.getInstance().getResources().openRawResource(resourceId);
            scanner = new Scanner(ins, CharEncoding.UTF_8);
            return scanner.useDelimiter("\\A").next();
        } finally {
            IOUtils.closeQuietly(ins);
            // Scanner does not implement Closeable on Android 4.1, so closeQuietly leads to crash there
            if (scanner != null) {
                scanner.close();
            }
        }
    }

}
