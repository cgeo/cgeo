package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.fragment.app.DialogFragment;

/**
 * Fullscreen dialogs should be used if the dialog meets any of the following criteria:
 *
 * - Dialogs that include components which require keyboard input, such as form fields
 * - When changes aren’t saved instantly
 * - When components within the dialog open additional dialogs
 *
 * See also:
 * https://material.io/components/dialogs/android#full-screen-dialog
 * https://medium.com/alexander-schaefer/implementing-the-new-material-design-full-screen-dialog-for-android-e9dcc712cb38
 */
public abstract class AbstractFullscreenDialog extends DialogFragment {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.cgeo_fullScreenDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            //prevent popup window to extend under the virtual keyboard or above the top of phone display (see #8793)
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}
