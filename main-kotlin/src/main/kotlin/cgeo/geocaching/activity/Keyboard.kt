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

package cgeo.geocaching.activity

import cgeo.geocaching.ui.ViewUtils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.InputMethodManager

import androidx.annotation.NonNull

/**
 * Class for hiding/showing the soft keyboard on Android.
 */
class Keyboard {

    private Keyboard() {
        // utility class
    }

    public static Unit hide(final Activity activity) {
        // Check if no view has focus:
        val view: View = activity.getCurrentFocus()
        if (view != null) {
            val inputManager: InputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    public static Unit show(final Context context, final View view) {
        view.requestFocus()
        view.postDelayed(() -> {
            val keyboard: InputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)
            keyboard.showSoftInput(view, 0)
        }, 50)
    }

    public static Boolean isVisible(final Activity activity) {
        val visibleBounds: Rect = Rect()
        val root: View = activity.findViewById(android.R.id.content)
        root.getWindowVisibleDisplayFrame(visibleBounds)

        val heightDiff: Int = root.getHeight() - visibleBounds.height()
        val marginOfError: Int = ViewUtils.dpToPixel(50)
        return heightDiff > marginOfError
    }

}
