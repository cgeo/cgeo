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

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.R

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment

/**
 * Fullscreen dialogs should be used if the dialog meets any of the following criteria:
 * <br>
 * - Dialogs that include components which require keyboard input, such as form fields
 * - When changes aren’t saved instantly
 * - When components within the dialog open additional dialogs
 * <br>
 * See also:
 * <a href="https://material.io/components/dialogs/android#full-screen-dialog">...</a>
 * <a href="https://medium.com/alexander-schaefer/implementing-the-new-material-design-full-screen-dialog-for-android-e9dcc712cb38">...</a>
 */
abstract class AbstractFullscreenDialog : DialogFragment() {
    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.cgeo_fullScreenDialog)
        WindowCompat.enableEdgeToEdge(requireActivity().getWindow())
    }

    protected Unit applyEdge2Edge(final View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            val innerPadding: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime())
            view.setPadding(innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom)
            return windowInsets
        })
    }

    override     public Unit onStart() {
        super.onStart()
        val dialog: Dialog = getDialog()
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }
}
