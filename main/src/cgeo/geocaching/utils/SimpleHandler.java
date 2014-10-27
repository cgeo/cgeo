package cgeo.geocaching.utils;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public abstract class SimpleHandler extends Handler {
    public static final String MESSAGE_TEXT = "message_text";
    protected final WeakReference<AbstractActivity> activityRef;
    protected final WeakReference<Progress> progressDialogRef;

    public SimpleHandler(final AbstractActivity activity, final Progress progress) {
        activityRef = new WeakReference<>(activity);
        progressDialogRef = new WeakReference<>(progress);
    }

    @Override
    public void handleMessage(final Message msg) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null && msg.getData() != null && msg.getData().getString(MESSAGE_TEXT) != null) {
            activity.showToast(msg.getData().getString(MESSAGE_TEXT));
        }
        final Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
