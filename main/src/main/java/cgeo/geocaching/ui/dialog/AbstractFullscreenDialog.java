package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

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
public abstract class AbstractFullscreenDialog extends DialogFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.cgeo_fullScreenDialog);
        WindowCompat.enableEdgeToEdge(requireActivity().getWindow());
    }

    protected void applyEdge2Edge(final View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            final Insets innerPadding = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom);
            return windowInsets;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
