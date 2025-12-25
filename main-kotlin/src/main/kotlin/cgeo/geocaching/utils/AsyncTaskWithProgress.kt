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
import android.app.ProgressDialog

/**
 * AsyncTask which automatically shows a progress dialog. The progress is tracked with integers.
 * <br>
 * Use it like the {@code AsyncTask} class, but leave away the middle template parameter. Override
 * {@link #doInBackgroundInternal(Object[])} and related methods.
 *
 * <p>
 * If no style is given, the progress dialog uses "determinate" style with known maximum. The progress maximum is
 * automatically derived from the number of {@code Params} given to the task in {@link #execute(Object...)}.
 * </p>
 *
 * <p>
 * Use {@code publishProgress(Integer)} to change the current progress.
 * </p>
 */
abstract class AsyncTaskWithProgress<Params, Result> : AbstractAsyncTaskWithProgress()<Params, Integer, Result> {

    private var indeterminate: Boolean = false

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage) {
        this(activity, progressTitle, progressMessage, false)
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle) {
        this(activity, progressTitle, null)
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage, final Boolean indeterminate) {
        super(activity, progressTitle, progressMessage)
        this.indeterminate = indeterminate
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final Boolean indeterminate) {
        this(activity, progressTitle, null, indeterminate)
    }

    /**
     * Show the progress dialog.
     */
    override     protected final Unit onPreExecute() {
        if (activity != null) {
            if (indeterminate) {
                progress.show(activity, progressTitle, progressMessage, true, null)
            } else {
                progress.show(activity, progressTitle, progressMessage, ProgressDialog.STYLE_HORIZONTAL, null)
            }
        }
        onPreExecuteInternal()
    }

    /**
     * Define the progress logic.
     *
     * @param status The progress status
     */
    override     protected final Unit onProgressUpdate(final Integer... status) {
        val progressValue: Int = status[0]
        if (activity != null && progressValue >= 0) {
            progress.setProgress(progressValue)
        }
        onProgressUpdateInternal(progressValue)
    }

    /**
     * Launch the process in background.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    @SuppressWarnings("unchecked")
    override     protected final Result doInBackground(final Params... params) {
        if (params != null) {
            progress.setMaxProgressAndReset(params.length)
        }
        return doInBackgroundInternal(params)
    }
}
