package cgeo.geocaching.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.WindowManager;

/**
 * progress dialog wrapper for easier management of resources
 */
public class Progress {

    private ProgressDialog dialog;
    private int progress = 0;
    private int progressDivider = 1;

    public synchronized void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = null;
    }

    public synchronized void show(final Context context, final String title, final String message, final boolean indeterminate, final Message cancelMessage) {
        if (dialog == null) {
            dialog = ProgressDialog.show(context, title, message, indeterminate, cancelMessage != null);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            dialog.setProgress(0);
            dialog.setCanceledOnTouchOutside(false);
            this.progress = 0;
            if (cancelMessage != null) {
                dialog.setCancelMessage(cancelMessage);
            }
        }
    }

    public synchronized void show(final Context context, final String title, final String message, final int style, final Message cancelMessage) {
        if (dialog == null) {
            dialog = new ProgressDialog(context);
            dialog.setProgress(0);
            dialog.setCanceledOnTouchOutside(false);
            this.progress = 0;
            dialog.setTitle(title);
            dialog.setMessage(message);
            dialog.setProgressStyle(style);
            if (cancelMessage != null) {
                dialog.setCancelable(true);
                dialog.setCancelMessage(cancelMessage);
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getResources().getString(android.R.string.cancel), cancelMessage);
            } else {
                dialog.setCancelable(false);
            }
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            dialog.show();
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

    public synchronized void setMaxProgressAndReset(final int max) {
        if (dialog != null && dialog.isShowing()) {
            final int modMax = max / this.progressDivider;
            dialog.setMax(modMax);
            dialog.setProgress(0);
        }
        this.progress = 0;
    }

    public synchronized void setProgress(final int progress) {
        final int modProgress = progress / this.progressDivider;
        if (dialog != null && dialog.isShowing()) {
            dialog.setProgress(modProgress);
        }
        this.progress = modProgress;
    }

    public synchronized int getProgress() {
        if (dialog != null) {
            dialog.getProgress();
        }
        return this.progress;
    }

    public synchronized void setProgressDivider(final int progressDivider) {
        this.progressDivider = progressDivider;
    }
}
