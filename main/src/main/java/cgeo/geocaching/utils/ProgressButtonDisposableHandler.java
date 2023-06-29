package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.ui.ViewUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec;
import com.google.android.material.progressindicator.IndeterminateDrawable;

public class ProgressButtonDisposableHandler extends SimpleDisposableHandler {
    public MaterialButton button;
    public Drawable originalIcon;

    public ProgressButtonDisposableHandler(final AbstractActivity activity) {
        super(activity, null);
    }

    public void showProgress(final MaterialButton button) {
        if (button != null) {
            this.button = button;
            originalIcon = button.getIcon();
            button.setIcon(getCircularProgressIndicatorDrawable(activityRef.get()));
            button.setEnabled(false);
        }
    }

    @Override
    protected final void dismissProgress() {
        if (button != null && originalIcon != null) {
            button.setIcon(originalIcon);
            button.setEnabled(true);
        }
    }

    private static IndeterminateDrawable<CircularProgressIndicatorSpec> getCircularProgressIndicatorDrawable(final Context context) {
        //final CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(context, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_CircularProgressIndicator_ExtraSmall);
        final CircularProgressIndicatorSpec spec = new CircularProgressIndicatorSpec(context, null);
        spec.indicatorSize = ViewUtils.dpToPixel(context.getResources().getDimension(R.dimen.buttonSize_iconButton) / context.getResources().getDisplayMetrics().density / 1.5f);
        return IndeterminateDrawable.createCircularDrawable(context, spec);
    }

}
