package cgeo.geocaching.playservices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

/**
 * No-op stub for the "foss" flavor, which deliberately avoids a Play Core/Play Services
 * dependency. The "basic" and "nojit" flavors get a real Play Feature Delivery backed
 * implementation of this same class from src/nofoss/java instead.
 */
public final class WherigoModuleInstaller {

    private WherigoModuleInstaller() {
        // utility class
    }

    public static boolean isSupported() {
        return false;
    }

    public static void installSplitCompat(@NonNull final Context context) {
        // no-op: on-demand split delivery isn't available in this build flavor
    }

    public static boolean isModuleInstalled(@NonNull final Context context) {
        return false;
    }

    public static void requestInstall(@NonNull final Activity activity, @NonNull final Runnable onSuccess, @NonNull final Consumer<String> onFailure) {
        onFailure.accept("on-demand module install isn't available in this build");
    }
}
