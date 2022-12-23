package cgeo.geocaching.utils;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;

import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;

public class SimpleDisposableHandler extends DisposableHandler {
    public static final String MESSAGE_TEXT = "message_text";
    private static final int DISPOSE_WITH_MESSAGE = -738434;
    protected final WeakReference<AbstractActivity> activityRef;
    protected final WeakReference<Progress> progressDialogRef;

    public SimpleDisposableHandler(final AbstractActivity activity, final Progress progress) {
        this.activityRef = new WeakReference<>(activity);
        this.progressDialogRef = new WeakReference<>(progress);
    }

    @Override
    protected void handleRegularMessage(final Message msg) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null && msg.getData() != null && msg.getData().getString(MESSAGE_TEXT) != null) {
            activity.showToast(msg.getData().getString(MESSAGE_TEXT));
        }
        dismissProgress();
        if (msg.what == DISPOSE_WITH_MESSAGE) {
            dispose();
        }
    }

    public Message cancelMessage(final String text) {
        final Message msg = obtainMessage(DISPOSE_WITH_MESSAGE);
        final Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_TEXT, text);
        msg.setData(bundle);
        return msg;
    }

    @Override
    protected void handleDispose() {
        dismissProgress();
    }

    protected final void showToast(@StringRes final int resId) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            final Resources res = activity.getResources();
            activity.showToast(res.getText(resId).toString());
        }
    }

    protected final void showToast(final String msg) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.showToast(msg);
        }
    }

    protected final void dismissProgress() {
        final Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected final void setProgressMessage(final String txt) {
        final Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.setMessage(txt);
        }
    }

    protected final void finishActivity() {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.finish();
        }

    }

    protected void updateStatusMsg(@StringRes final int resId, final String msg) {
        final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
        if (activity != null) {
            setProgressMessage(activity.getString(resId)
                    + "\n\n"
                    + msg);
        }
    }

    public void sendTextMessage(final int what, @StringRes final int resId) {
        final CacheDetailActivity activity = (CacheDetailActivity) activityRef.get();
        if (activity != null) {
            final Message msg = obtainMessage(what);
            final Bundle bundle = new Bundle();
            bundle.putString(MESSAGE_TEXT, activity.getString(resId));
            msg.setData(bundle);
            msg.sendToTarget();
        }
    }

}
