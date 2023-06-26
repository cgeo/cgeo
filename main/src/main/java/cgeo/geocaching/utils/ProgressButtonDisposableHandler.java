package cgeo.geocaching.utils;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.ViewUtils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;

import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

public class ProgressButtonDisposableHandler extends DisposableHandler {
    public static final String MESSAGE_TEXT = "message_text";
    private static final int DISPOSE_WITH_MESSAGE = -738434;
    protected final WeakReference<AbstractActivity> activityRef;
    public MaterialButton button;
    public Drawable originalIcon;

    public ProgressButtonDisposableHandler(final AbstractActivity activity) {
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

    public void showProgress(final MaterialButton button) {
        if (button != null) {
            this.button = button;
            originalIcon = button.getIcon();
            button.setIcon(getCircularProgressIndicatorDrawable(activityRef.get()));
            button.setEnabled(false);
        }
    }

    protected final void dismissProgress() {
        dismissProgress(null);
    }

    protected final void dismissProgress(@StringRes final Integer resId) {
        if (button != null && originalIcon != null) {
            button.setIcon(originalIcon);
            button.setEnabled(true);
        }
        if (resId != null) {
            final AbstractActivity activity = activityRef.get();
            if (activity != null) {
                showShortToast(activity.getString(resId));
            }
        } else {
            showShortToast("done");
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

    private static IndeterminateDrawable getCircularProgressIndicatorDrawable(final Context context) {
        final CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(context, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_CircularProgressIndicator_ExtraSmall);
        spec.indicatorSize = ViewUtils.dpToPixel(context.getResources().getDimension(R.dimen.buttonSize_iconButton) / context.getResources().getDisplayMetrics().density / 1.5f);
        return IndeterminateDrawable.createCircularDrawable(context, spec);
    }

}
