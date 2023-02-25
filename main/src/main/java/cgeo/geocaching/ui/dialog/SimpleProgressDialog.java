package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.progressindicator.LinearProgressIndicator;

public class SimpleProgressDialog {

    protected final AlertDialog.Builder builder;
    protected AlertDialog dialog = null;
    private TextView textViewMessage = null;
    private LinearProgressIndicator progressBar = null;
    private TextView textViewAdditionalInfo = null;
    private final Handler handler;

    public SimpleProgressDialog(final Context context) {
        builder = Dialogs.newBuilder(context);
        builder.setView(R.layout.progressdialog);
        handler = new Handler(Looper.getMainLooper());
    }

    public SimpleProgressDialog(final Context context, final TextParam title) {
        this(context);
        builder.setTitle(title.getText(context));
    }

    public void setButton(final int whichButton, final TextParam label, final DialogInterface.OnClickListener listener) {
        if (dialog == null) {
            dialog = builder.create();
        }
        dialog.setButton(whichButton, label.getText(dialog.getContext()), listener);
    }

    public AlertDialog show() {
        if (dialog == null) {
            dialog = builder.create();
        }
        dialog.show();
        return dialog;
    }

    public void dismiss() {
        if (dialog == null) {
            return;
        }
        dialog.dismiss();
        dialog = null;
        textViewMessage = null;
        progressBar = null;
    }

    public void setMessage(final TextParam message) {
        if (dialog == null) {
            return;
        }
        if (textViewMessage == null) {
            textViewMessage = dialog.findViewById(R.id.message);
        }
        if (textViewMessage != null) {
            message.applyTo(textViewMessage);
        }
    }

    public void postAdditionalInfo(final TextParam message) {
        handler.post(() -> {
            if (dialog == null) {
                return;
            }
            if (textViewAdditionalInfo == null) {
                textViewAdditionalInfo = dialog.findViewById(R.id.additionalinfo);
            }
            if (textViewAdditionalInfo != null) {
                message.applyTo(textViewAdditionalInfo);
            }
        });
    }

    public void setTypeDeterminate(final int max) {
        if (dialog == null) {
            return;
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar);
        }
        if (progressBar != null) {
            progressBar.setMax(max);
            progressBar.setProgress(0);
            progressBar.setIndeterminate(false);
        }
    }

    /* only valid when setTypeDeterminate has been called before */
    public void postProgress(final int progress) {
        handler.post(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
        });
    }

    public void setTypeIndeterminate() {
        if (dialog == null) {
            return;
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar);
        }
        if (progressBar != null) {
            progressBar.setIndeterminate(true);
        }
    }

    public void setProgressVisibility(final int visibility) {
        if (dialog == null) {
            return;
        }
        if (progressBar == null) {
            progressBar = dialog.findViewById(R.id.progressbar);
        }
        if (progressBar != null) {
            progressBar.setVisibility(visibility);
        }
    }
}
