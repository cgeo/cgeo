package cgeo.geocaching.utils;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public abstract class SimpleHandler extends Handler {
    public static final String MESSAGE_TEXT = "message_text";
    protected final WeakReference<AbstractActivity> activityRef;
    protected final WeakReference<Progress> progressDialogRef;

    public SimpleHandler(final AbstractActivity activity, final Progress progress) {
        this.activityRef = new WeakReference<AbstractActivity>(activity);
        this.progressDialogRef = new WeakReference<Progress>(progress);
    }

    @Override
    public void handleMessage(final Message msg) {
        AbstractActivity activity = activityRef.get();
        if (activity != null && msg.getData() != null && msg.getData().getString(MESSAGE_TEXT) != null) {
            activity.showToast(msg.getData().getString(MESSAGE_TEXT));
        }
        dismissProgress();
    }

    protected final void showToast(final int resId) {
        AbstractActivity activity = activityRef.get();
        if (activity != null) {
            Resources res = activity.getResources();
            activity.showToast(res.getText(resId).toString());
        }
    }

    protected final void dismissProgress() {
        Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected final void setProgressMessage(final String txt) {
        Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.setMessage(txt);
        }
    }

    protected final void finishActivity() {
        AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.finish();
        }

    }

}
