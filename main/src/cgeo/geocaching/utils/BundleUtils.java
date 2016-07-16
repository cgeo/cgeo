package cgeo.geocaching.utils;

import android.support.annotation.NonNull;

import android.os.Bundle;

public class BundleUtils {

    private BundleUtils() {
        // utility class
    }

    @NonNull
    public static String getString(final Bundle bundle, @NonNull final String key, @NonNull final String defaultValue) {
        final String res = bundle.getString(key);
        if (res != null) {
            return res;
        }
        return defaultValue;
    }
}
