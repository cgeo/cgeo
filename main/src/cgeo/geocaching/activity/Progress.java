package cgeo.geocaching.activity;

import cgeo.geocaching.ui.dialog.CustomProgressDialog;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;

/**
 * progress dialog wrapper for easier management of resources
 */
public class Progress {

    private ProgressDialog dialog;
    private int progress = 0;
    private final boolean hideAbsolute;
    private DialogInterface.OnClickListener cancelListener;

    public Progress(final boolean hideAbsolute) {
        this.hideAbsolute = hideAbsolute;
    }

    public Progress() {
        this(false);
    }

    public synchronized void dismiss() {
        if (isShowing()) {
            try {
                dialog.dismiss();
            } catch (final Exception e) {
                Log.e("Progress.dismiss", e);
            }
        }
        dialog = null;
    }

    public synchronized void show(final Context context, final String title, final String message, final boolean indeterminate, final Message cancelMessage) {
        if (!isShowing()) {
            createProgressDialog(context, title, message, cancelMessage);
            dialog.setIndeterminate(indeterminate);
            dialog.show();
        }
    }

    public synchronized void show(final Context context, final String title, final String message, final int style, final Message cancelMessage) {
        if (!isShowing()) {
            createProgressDialog(context, title, message, cancelMessage);
            dialog.setProgressStyle(style);
            dialog.show();
        }
    }

    private void createProgressDialog(final Context context, final String title, final String message, final Message cancelMessage) {
        dialog = hideAbsolute ? new CustomProgressDialog(context) : new ProgressDialog(context);
        dialog.setTitle(title);
        dialog.setMessage(message);
        if (cancelMessage != null) {
            dialog.setCancelable(true);
            dialog.setCancelMessage(cancelMessage);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), cancelMessage);
        } else if (cancelListener != null) {
            dialog.setCancelable(true);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), cancelListener);
        } else {
            dialog.setCancelable(false);
        }
        dialog.setProgress(0);
        dialog.setCanceledOnTouchOutside(false);
        final Window window = dialog.getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        progress = 0;
    }

    public synchronized void setMessage(final String message) {
        if (dialog != null && dialog.isShowing()) {
            dialog.setMessage(message);
        }
    }

    public synchronized boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    public synchronized void setMaxProgressAndReset(final int max) {
        if (isShowing()) {
            dialog.setMax(max);
            dialog.setProgress(0);
        }
        progress = 0;
    }

    public synchronized void setProgress(final int progress) {
        this.progress = progress;
        if (isShowing()) {
            dialog.setProgress(progress);
        }
    }

    public int getProgress() {
        return progress;
    }

    public void incrementProgressBy(final int increment) {
        setProgress(progress + increment);
    }

    public void setOnCancelListener(final DialogInterface.OnClickListener cancelListener) {
        this.cancelListener = cancelListener;
    }
}
