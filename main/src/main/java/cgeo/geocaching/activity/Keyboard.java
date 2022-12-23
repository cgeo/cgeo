package cgeo.geocaching.activity;

import cgeo.geocaching.ui.ViewUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

/**
 * Class for hiding/showing the soft keyboard on Android.
 */
public class Keyboard {

    private Keyboard() {
        // utility class
    }

    public static void hide(@NonNull final Activity activity) {
        // Check if no view has focus:
        final View view = activity.getCurrentFocus();
        if (view != null) {
            final InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public static void show(@NonNull final Context context, final View view) {
        view.requestFocus();
        view.postDelayed(() -> {
            final InputMethodManager keyboard = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, 0);
        }, 50);
    }

    public static boolean isVisible(@NonNull final Activity activity) {
        final Rect visibleBounds = new Rect();
        final View root = activity.findViewById(android.R.id.content);
        root.getWindowVisibleDisplayFrame(visibleBounds);

        final int heightDiff = root.getHeight() - visibleBounds.height();
        final int marginOfError = ViewUtils.dpToPixel(50);
        return heightDiff > marginOfError;
    }

}
