package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;

import android.view.View;

public class ProgressBarDisposableHandler extends SimpleDisposableHandler {

    public ProgressBarDisposableHandler(final AbstractActivity activity) {
        super(activity, null);
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

    @Override
    protected final void dismissProgress() {
        dismissProgress(null);
    }

    protected final void dismissProgress(final String text) {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            final View progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (text != null) {
                activity.showShortToast(text);
            }
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
}
