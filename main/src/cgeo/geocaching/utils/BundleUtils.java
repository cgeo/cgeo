package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Bundle;

public class BundleUtils {

    @NonNull
    public static String getString(Bundle bundle, @NonNull String key, @NonNull String defaultValue) {
        String res = bundle.getString(key);
        if (res != null) {
            return res;
        }
        return defaultValue;
    }
}
