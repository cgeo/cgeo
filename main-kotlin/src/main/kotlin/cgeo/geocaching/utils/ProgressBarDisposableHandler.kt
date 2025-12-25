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

import android.view.View

class ProgressBarDisposableHandler : SimpleDisposableHandler() {

    public ProgressBarDisposableHandler(final AbstractActivity activity) {
        super(activity, null)
    }

    public final Unit showProgress() {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null) {
            val progressBar: View = activity.findViewById(R.id.progressBar)
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE)
            }
        }
    }

    override     protected final Unit dismissProgress() {
        dismissProgress(null)
    }

    protected final Unit dismissProgress(final String text) {
        val activity: AbstractActivity = activityRef.get()
        if (activity != null) {
            val progressBar: View = activity.findViewById(R.id.progressBar)
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE)
            }
            if (text != null) {
                activity.showShortToast(text)
            }
        }
    }

    public static Boolean isInProgress(final AbstractActivity activity) {
        if (activity != null) {
            val progressBar: View = activity.findViewById(R.id.progressBar)
            if (progressBar != null) {
                return progressBar.getVisibility() == View.VISIBLE
            }
        }
        return false
    }
}
