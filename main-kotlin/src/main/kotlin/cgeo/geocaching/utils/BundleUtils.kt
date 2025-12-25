// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import android.os.Bundle

import androidx.annotation.NonNull

class BundleUtils {

    private BundleUtils() {
        // utility class
    }

    public static String getString(final Bundle bundle, final String key, final String defaultValue) {
        val res: String = bundle.getString(key)
        if (res != null) {
            return res
        }
        return defaultValue
    }
}
