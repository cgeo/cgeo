package cgeo.geocaching.utils;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Message;
import android.view.View;

import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;

public class ProgressBarDisposableHandler extends DisposableHandler {
    public static final String MESSAGE_TEXT = "message_text";
    private static final int DISPOSE_WITH_MESSAGE = -738434;
    protected final WeakReference<AbstractActivity> activityRef;

    public ProgressBarDisposableHandler(final AbstractActivity activity) {
        this.activityRef = new WeakReference<>(activity);
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

    protected final void showShortToast(final String msg) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.showShortToast(msg);
        }
    }

    public final void showProgress() {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            final View progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    protected final void dismissProgress() {
        dismissProgress(null);
    }

    protected final void dismissProgress(@StringRes final Integer resId) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            final View progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
        if (resId != null) {
            showShortToast(activity.getString(resId));
        }
    }

    public static boolean isInProgress(final AbstractActivity activity) {
        if (activity != null) {
            final View progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                return progressBar.getVisibility() == View.VISIBLE;
            }
        }
        return false;
    }

    protected final void finishActivity() {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            activity.finish();
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
