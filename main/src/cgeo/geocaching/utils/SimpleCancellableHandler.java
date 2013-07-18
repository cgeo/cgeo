package cgeo.geocaching.utils;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Progress;

import android.content.res.Resources;
import android.os.Message;

import java.lang.ref.WeakReference;

public class SimpleCancellableHandler extends CancellableHandler {
    public static final String SUCCESS_TEXT = "success_message";
    protected final WeakReference<AbstractActivity> activityRef;
    protected final WeakReference<Progress> progressDialogRef;

    public SimpleCancellableHandler(final AbstractActivity activity, final Progress progress) {
        this.activityRef = new WeakReference<AbstractActivity>(activity);
        this.progressDialogRef = new WeakReference<Progress>(progress);
    }

    @Override
    public void handleRegularMessage(final Message msg) {
        AbstractActivity activity = activityRef.get();
        if (activity != null && msg.getData() != null && msg.getData().getString(SUCCESS_TEXT) != null) {
            activity.showToast(msg.getData().getString(SUCCESS_TEXT));
        }
        Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        return;
    }

    @Override
    public void handleCancel(final Object extra) {
        AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.showToast((String) extra);
        }
        Progress progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    public final void showToast(int resId) {
        AbstractActivity activity = activityRef.get();
        if (activity != null) {
            Resources res = activity.getResources();
            activity.showToast(res.getText(resId).toString());
        }
    }

    public final void dismissProgress() {
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

    protected void updateStatusMsg(final int resId, final String msg) {
        CacheDetailActivity activity = ((CacheDetailActivity) activityRef.get());
        if (activity != null) {
            setProgressMessage(activity.getResources().getString(resId)
                    + "\n\n"
                    + msg);
        }
    }

}
