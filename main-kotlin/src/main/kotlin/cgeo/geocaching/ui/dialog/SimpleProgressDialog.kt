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
import cgeo.geocaching.ui.TextParam

import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import android.widget.TextView

import androidx.appcompat.app.AlertDialog

import com.google.android.material.progressindicator.LinearProgressIndicator

class SimpleProgressDialog {

    protected final AlertDialog.Builder builder
    protected var dialog: AlertDialog = null
    private var textViewMessage: TextView = null
    private var progressBar: LinearProgressIndicator = null
    private var textViewAdditionalInfo: TextView = null
    private final Handler handler

    public SimpleProgressDialog(final Context context) {
        builder = Dialogs.newBuilder(context)
        builder.setView(R.layout.progressdialog)
        handler = Handler(Looper.getMainLooper())
    }

    public SimpleProgressDialog(final Context context, final TextParam title) {
        this(context)
        builder.setTitle(title.getText(context))
    }

    public Unit setButton(final Int whichButton, final TextParam label, final DialogInterface.OnClickListener listener) {
        if (dialog == null) {
            dialog = builder.create()
        }
        dialog.setButton(whichButton, label.getText(dialog.getContext()), listener)
    }

    public AlertDialog show() {
        if (dialog == null) {
            dialog = builder.create()
        }
        dialog.show()
        return dialog
    }

    public Unit dismiss() {
        if (dialog == null) {
            return
        }
        dialog.dismiss()
        dialog = null
        textViewMessage = null
        progressBar = null
    }

    public Unit setMessage(final TextParam message) {
        if (dialog == null) {
            return
        }
        if (textViewMessage == null) {
            textViewMessage = dialog.findViewById(R.id.message)
        }
        if (textViewMessage != null) {
            message.applyTo(textViewMessage)
        }
    }

    public Unit postAdditionalInfo(final TextParam message) {
        handler.post(() -> {
            if (dialog == null) {
                return
            }
            if (textViewAdditionalInfo == null) {
                textViewAdditionalInfo = dialog.findViewById(R.id.additionalinfo)
            }
            if (textViewAdditionalInfo != null) {
                message.applyTo(textViewAdditionalInfo)
            }
        })
    }

    public Unit setTypeDeterminate(final Int max) {
        if (dialog == null) {
            return
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar)
        }
        if (progressBar != null) {
            progressBar.setMax(max)
            progressBar.setProgress(0)
            progressBar.setIndeterminate(false)
        }
    }

    /* only valid when setTypeDeterminate has been called before */
    public Unit postProgress(final Int progress) {
        handler.post(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress)
            }
        })
    }

    public Unit setTypeIndeterminate() {
        if (dialog == null) {
            return
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar)
        }
        if (progressBar != null) {
            progressBar.setIndeterminate(true)
        }
    }

    public Unit setProgressVisibility(final Int visibility) {
        if (dialog == null) {
            return
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar)
        }
        if (progressBar != null) {
            progressBar.setVisibility(visibility)
        }
    }
}
