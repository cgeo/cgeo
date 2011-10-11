package cgeo.geocaching.activity;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * progress dialog wrapper for easier management of resources
 */
public class Progress {

    private static ProgressDialog dialog;

    public static void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    public static ProgressDialog show(Context context, String title, String message, boolean indeterminate, boolean cancelable) {
        if (dialog == null) {
            dialog = ProgressDialog.show(context, title, message, indeterminate, cancelable);
        }
        return dialog;
    }

    public static void setMessage(final String message) {
        if (dialog != null && dialog.isShowing()) {
            dialog.setMessage(message);
        }
    }

    public static boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

}
