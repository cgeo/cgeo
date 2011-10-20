package cgeo.geocaching.activity;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * progress dialog wrapper for easier management of resources
 */
public class Progress {

    private ProgressDialog dialog;

    public synchronized void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    public synchronized void show(Context context, String title, String message, boolean indeterminate, boolean cancelable) {
        if (dialog == null) {
            dialog = ProgressDialog.show(context, title, message, indeterminate, cancelable);
        }
    }

    public synchronized void setMessage(final String message) {
        if (dialog != null && dialog.isShowing()) {
            dialog.setMessage(message);
        }
    }

    public synchronized boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

}
