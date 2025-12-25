// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.ui.ViewUtils

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Message

import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable

class ProgressButtonDisposableHandler : SimpleDisposableHandler() {
    private static val MESSAGE_SUCCEEDED: Int = 1
    public MaterialButton button
    public Drawable originalIcon

    public ProgressButtonDisposableHandler(final AbstractActivity activity) {
        super(activity, null)
    }

    override     protected Unit handleRegularMessage(final Message msg) {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null && msg.getData() != null && msg.getData().getString(MESSAGE_TEXT) != null) {
            if (msg.what == MESSAGE_SUCCEEDED) {
                activity.showShortToast(msg.getData().getString(MESSAGE_TEXT))
            } else {
                activity.showToast(msg.getData().getString(MESSAGE_TEXT))
            }
        }
        dismissProgress()
    }

    public Unit showProgress(final MaterialButton button) {
        if (button != null) {
            this.button = button
            originalIcon = button.getIcon()
            button.setIcon(getCircularProgressIndicatorDrawable(activityRef.get()))
            button.setEnabled(false)
        }
    }

    override     protected final Unit dismissProgress() {
        if (button != null && originalIcon != null) {
            button.setIcon(originalIcon)
            button.setEnabled(true)
        }
    }

    private static IndeterminateDrawable<CircularProgressIndicatorSpec> getCircularProgressIndicatorDrawable(final Context context) {
        val spec: CircularProgressIndicatorSpec = CircularProgressIndicatorSpec(context, null, 0, com.google.android.material.R.style.Widget_MaterialComponents_CircularProgressIndicator_Small)
        spec.indicatorSize = ViewUtils.dpToPixel(context.getResources().getDimension(R.dimen.buttonSize_iconButton) / context.getResources().getDisplayMetrics().density / 1.8f)
        return IndeterminateDrawable.createCircularDrawable(context, spec)
    }

}
