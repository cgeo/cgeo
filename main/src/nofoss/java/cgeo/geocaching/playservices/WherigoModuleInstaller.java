package cgeo.geocaching.playservices;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;

/**
 * Real Play Feature Delivery backed implementation, used by the "basic" and "nojit" flavors.
 * The "foss" flavor gets a no-op stub of this same class from src/foss/java instead, since it
 * deliberately avoids a Play Core/Play Services dependency.
 */
public final class WherigoModuleInstaller {

    private static final String MODULE_WHERIGO = "wherigo";

    private WherigoModuleInstaller() {
        // utility class
    }

    /** Whether an on-demand install flow is available in this build flavor. */
    public static boolean isSupported() {
        return true;
    }

    /** Makes classes/resources of already-installed splits available without an app restart. */
    public static void installSplitCompat(@NonNull final Context context) {
        SplitCompat.install(context);
    }

    public static boolean isModuleInstalled(@NonNull final Context context) {
        return SplitInstallManagerFactory.create(context).getInstalledModules().contains(MODULE_WHERIGO);
    }

    /**
     * Requests on-demand install of the Wherigo module. Exactly one of {@code onSuccess} or
     * {@code onFailure} is called, on the main thread.
     */
    public static void requestInstall(@NonNull final Activity activity, @NonNull final Runnable onSuccess, @NonNull final Consumer<String> onFailure) {
        final SplitInstallManager manager = SplitInstallManagerFactory.create(activity);
        final SplitInstallStateUpdatedListener[] listenerHolder = new SplitInstallStateUpdatedListener[1];

        listenerHolder[0] = state -> {
            if (!state.moduleNames().contains(MODULE_WHERIGO)) {
                return;
            }
            final int status = state.status();
            if (status == SplitInstallSessionStatus.INSTALLED) {
                manager.unregisterListener(listenerHolder[0]);
                SplitCompat.install(activity);
                onSuccess.run();
            } else if (status == SplitInstallSessionStatus.FAILED || status == SplitInstallSessionStatus.CANCELED) {
                manager.unregisterListener(listenerHolder[0]);
                onFailure.accept("status=" + status + ", errorCode=" + state.errorCode());
            }
            // other statuses (PENDING/DOWNLOADING/DOWNLOADED/INSTALLING/...) just mean "still working"
        };

        manager.registerListener(listenerHolder[0]);
        manager.startInstall(SplitInstallRequest.newBuilder().addModule(MODULE_WHERIGO).build())
                .addOnFailureListener(e -> {
                    manager.unregisterListener(listenerHolder[0]);
                    onFailure.accept(e.getMessage());
                });
    }
}
