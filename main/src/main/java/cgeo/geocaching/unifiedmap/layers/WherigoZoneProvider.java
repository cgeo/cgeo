package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.utils.ListenerHelper;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * In-process registration point the {@code :wherigo} feature module uses to hand live game state
 * back to the base app's map, without the base app ever depending on the feature module's code.
 * <p>
 * The base app can't import Wherigo classes at compile time (dynamic feature modules may only
 * depend on the base module, never the reverse), but at runtime both live in the same process, so
 * a plain in-process callback registration is enough here - no real IPC/multi-process mechanism is
 * needed. The {@code :wherigo} module calls {@link #register} once its singleton game state is
 * created, and {@link #notifyChanged()} whenever the running game's zones change.
 */
public final class WherigoZoneProvider {

    /** Backend supplied by the wherigo module, or {@code null} while it hasn't loaded / registered yet. */
    private interface Backend {
        @NonNull List<WherigoZoneInfo> getZones();

        boolean isPlaying();

        void onZoneTapped(@NonNull Activity activity, @NonNull String zoneName);

        void showQuickView(@NonNull Activity activity);
    }

    @Nullable private static volatile Backend backend;

    private static final ListenerHelper<Runnable> CHANGE_LISTENERS = new ListenerHelper<>();

    private WherigoZoneProvider() {
        // utility class
    }

    public static void register(@NonNull final Supplier<List<WherigoZoneInfo>> zoneSupplier,
                                 @NonNull final Supplier<Boolean> isPlayingSupplier,
                                 @NonNull final BiConsumer<Activity, String> onZoneTapped,
                                 @NonNull final Consumer<Activity> showQuickView) {
        backend = new Backend() {
            @NonNull
            @Override
            public List<WherigoZoneInfo> getZones() {
                return zoneSupplier.get();
            }

            @Override
            public boolean isPlaying() {
                return isPlayingSupplier.get();
            }

            @Override
            public void onZoneTapped(@NonNull final Activity activity, @NonNull final String zoneName) {
                onZoneTapped.accept(activity, zoneName);
            }

            @Override
            public void showQuickView(@NonNull final Activity activity) {
                showQuickView.accept(activity);
            }
        };
    }

    @NonNull
    public static List<WherigoZoneInfo> getZones() {
        final Backend b = backend;
        return b == null ? Collections.emptyList() : b.getZones();
    }

    public static boolean isPlaying() {
        final Backend b = backend;
        return b != null && b.isPlaying();
    }

    public static void onZoneTapped(@NonNull final Activity activity, @NonNull final String zoneName) {
        final Backend b = backend;
        if (b != null) {
            b.onZoneTapped(activity, zoneName);
        }
    }

    public static void showQuickView(@NonNull final Activity activity) {
        final Backend b = backend;
        if (b != null) {
            b.showQuickView(activity);
        }
    }

    /** Called by the wherigo module whenever the running game's zones (or playing state) change. */
    public static void notifyChanged() {
        CHANGE_LISTENERS.executeOnMain(Runnable::run);
    }

    public static int addChangeListener(@NonNull final Runnable listener) {
        return CHANGE_LISTENERS.addListener(listener);
    }

    public static void removeChangeListener(final int listenerId) {
        CHANGE_LISTENERS.removeListener(listenerId);
    }
}
