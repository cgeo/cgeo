package cgeo.geocaching.activity;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

/**
 * Class for hiding/showing the soft keyboard on Android.
 *
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
}
