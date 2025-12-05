package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;

import android.view.View;

public class ProgressBarDisposableHandler extends SimpleDisposableHandler {

    private int colorStaticDivider;

    public ProgressBarDisposableHandler(final AbstractActivity activity) {
        super(activity, null);
        final View staticDivider = activity.findViewById(R.id.static_divider);
        if (staticDivider != null) {
            this.colorStaticDivider = staticDivider.getDrawingCacheBackgroundColor();
        }
    }

    public final void showProgress() {
        final AbstractActivity activity = activityRef.get();
        if (activity != null) {
            final View progressBar = activity.findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                final View staticDivider = activity.findViewById(R.id.static_divider);
                if (staticDivider != null) {
                    staticDivider.setBackgroundColor(activity.getResources().getColor(R.color.colorBackgroundTransparent));
                }
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
                final View staticDivider = activity.findViewById(R.id.static_divider);
                if (staticDivider != null) {
                    staticDivider.setBackgroundColor(this.colorStaticDivider);
                }
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
