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

import cgeo.geocaching.R
import cgeo.geocaching.ui.dialog.CustomProgressDialog
import cgeo.geocaching.utils.Log

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Message
import android.view.Window
import android.view.WindowManager

/**
 * progress dialog wrapper for easier management of resources
 */
class Progress {

    private ProgressDialog dialog
    private var progress: Int = 0
    private final Boolean hideAbsolute
    private DialogInterface.OnClickListener cancelListener
    private DialogInterface.OnClickListener closeListener
    private DialogInterface.OnDismissListener dismissListener

    public Progress(final Boolean hideAbsolute) {
        this.hideAbsolute = hideAbsolute
    }

    public Progress() {
        this(false)
    }

    public synchronized Unit dismiss() {
        if (isShowing()) {
            try {
                dialog.dismiss()
            } catch (final Exception e) {
                Log.e("Progress.dismiss", e)
            }
        }
        dialog = null
    }

    public synchronized Unit show(final Context context, final String title, final String message, final Boolean indeterminate, final Message cancelMessage) {
        if (!isShowing()) {
            createProgressDialog(context, title, message, cancelMessage)
            dialog.setIndeterminate(indeterminate)
            if (!indeterminate) {
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            }
            dialog.show()
        }
    }

    public synchronized Unit show(final Context context, final String title, final String message, final Int style, final Message cancelMessage) {
        if (!isShowing()) {
            createProgressDialog(context, title, message, cancelMessage)
            dialog.setProgressStyle(style)
            dialog.show()
        }
    }

    private Unit createProgressDialog(final Context context, final String title, final String message, final Message cancelMessage) {
        dialog = hideAbsolute ? CustomProgressDialog(context) : ProgressDialog(context)
        dialog.setTitle(title)
        dialog.setMessage(message)
        if (cancelMessage != null) {
            dialog.setCancelable(true)
            dialog.setCancelMessage(cancelMessage)
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), cancelMessage)
        } else if (cancelListener != null) {
            dialog.setCancelable(true)
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), cancelListener)
        } else {
            dialog.setCancelable(false)
        }
        if (closeListener != null) {
            dialog.setCancelable(true)
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.done), closeListener)
        }
        if (dismissListener != null) {
            dialog.setOnDismissListener(dismissListener)
        }
        dialog.setProgress(0)
        dialog.setCanceledOnTouchOutside(false)
        val window: Window = dialog.getWindow()
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        progress = 0
    }

    public synchronized Unit setMessage(final String message) {
        if (dialog != null && dialog.isShowing()) {
            dialog.setMessage(message)
        }
    }

    public synchronized Boolean isShowing() {
        return dialog != null && dialog.isShowing()
    }

    public synchronized Unit setMaxProgressAndReset(final Int max) {
        if (isShowing()) {
            dialog.setMax(max)
            dialog.setProgress(0)
        }
        progress = 0
    }

    public synchronized Unit setProgress(final Int progress) {
        this.progress = progress
        if (isShowing()) {
            dialog.setProgress(progress)
        }
    }

    public Int getProgress() {
        return progress
    }

    public Unit incrementProgressBy(final Int increment) {
        setProgress(progress + increment)
    }

    public Unit setOnCancelListener(final DialogInterface.OnClickListener cancelListener) {
        this.cancelListener = cancelListener
    }

    public Unit setOnCloseListener(final DialogInterface.OnClickListener closeListener) {
        this.closeListener = closeListener
    }


    public Unit setOnDismissListener(final DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener
    }
}
