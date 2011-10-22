package cgeo.geocaching.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Message;

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

    public synchronized void show(final Context context, final String title, final String message, final boolean indeterminate, final Message cancelMessage) {
        if (dialog == null) {
            dialog = ProgressDialog.show(context, title, message, indeterminate, cancelMessage != null);
            if (cancelMessage != null) {
                dialog.setCancelMessage(cancelMessage);
            }
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
