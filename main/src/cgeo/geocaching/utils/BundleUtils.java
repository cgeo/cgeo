package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Bundle;

public class BundleUtils {

    @NonNull
    public static String getString(final Bundle bundle, @NonNull final String key, @NonNull final String defaultValue) {
        final String res = bundle.getString(key);
        if (res != null) {
            return res;
        }
        return defaultValue;
    }
}
