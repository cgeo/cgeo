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

import android.app.Activity

import org.apache.commons.lang3.StringUtils

/**
 * AsyncTask which automatically shows a progress dialog. The progress is tracked with text messages.
 * <br>
 * Use it like the {@code AsyncTask} class, but leave away the middle template parameter. Override
 * {@link #doInBackgroundInternal(Object[])} and related methods.
 *
 * <p>
 * Use {@code publishProgress(String)} to change the text message.
 * </p>
 */
abstract class AsyncTaskWithProgressText<Params, Result> : AbstractAsyncTaskWithProgress()<Params, String, Result> {

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgressText(final Activity activity, final String progressTitle, final String progressMessage) {
        super(activity, progressTitle, progressMessage)
    }

    /**
     * Define the progress logic.
     *
     * @param status The progress status
     */
    override     protected final Unit onProgressUpdate(final String... status) {
        val progressValue: String = status[0]
        if (activity != null && StringUtils.isNotBlank(progressValue)) {
            progress.setMessage(progressValue)
        }
        onProgressUpdateInternal(progressValue)
    }
}
