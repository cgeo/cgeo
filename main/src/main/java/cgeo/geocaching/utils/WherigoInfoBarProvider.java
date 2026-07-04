package cgeo.geocaching.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Function;

/**
 * Lets the {@code :wherigo} module contribute its live info bar view without the base app
 * depending on wherigo classes at compile time.
 */
public final class WherigoInfoBarProvider {

    @Nullable private static volatile Function<Context, View> factory;

    private WherigoInfoBarProvider() {
        // utility class
    }

    public static void register(@NonNull final Function<Context, View> viewFactory) {
        factory = viewFactory;
    }

    /** Inflates the live Wherigo info bar into the given (empty) container, if the module is loaded. */
    public static void attach(@NonNull final ViewGroup container) {
        final Function<Context, View> f = factory;
        if (f != null && container.getChildCount() == 0) {
            container.addView(f.apply(container.getContext()));
        }
    }
}
