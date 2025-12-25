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

import cgeo.geocaching.activity.Progress

import android.app.Activity
import android.content.DialogInterface
import android.os.AsyncTask

import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Abstract AsyncTask which automatically shows a progress dialog. Use it like the {@code AsyncTask} class, but leave away the
 * middle template parameter. Override {@link #doInBackgroundInternal(Object[])} and related methods.
 * <br>
 * Do not use this class directly, instead use either AsyncTaskWithProgress or AsyncTaskWithProgressText.
 */
abstract class AbstractAsyncTaskWithProgress<Params, T, Result> : AsyncTask()<Params, T, Result> {

    protected val progress: Progress = Progress()
    protected final Activity activity
    protected final String progressTitle
    protected final String progressMessage

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AbstractAsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage) {
        this.activity = activity
        this.progressTitle = progressTitle
        this.progressMessage = progressMessage
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AbstractAsyncTaskWithProgress(final Activity activity, final String progressTitle) {
        this(activity, progressTitle, null)
    }

    /**
     * Show the progress dialog.
     */
    override     protected Unit onPreExecute() {
        if (activity != null) {
            progress.show(activity, progressTitle, progressMessage, true, null)
        }
        onPreExecuteInternal()
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onPreExecute()}.
     */
    @SuppressWarnings("EmptyMethod")
    protected Unit onPreExecuteInternal() {
        // empty by default
    }

    /**
     * Hide the progress dialog.
     * This method won't be invoked if the task was cancelled.
     *
     * @param result The result of the operation computed by doInBackground(Params...).
     */
    override     protected final Unit onPostExecute(final Result result) {
        onPostExecuteInternal(result)
        if (progress.isShowing()) {
            progress.dismiss()
        }
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onPostExecute(Object)}.
     *
     * @param result The result of the operation computed by {@link #doInBackground(Object...)}.
     */
    protected Unit onPostExecuteInternal(final Result result) {
        // empty by default
    }

    /**
     * Subclasses must implement the logic for the progress updates.
     *
     * @param status The progress status
     */
    @SuppressWarnings("unchecked")
    override     protected Unit onProgressUpdate(final T... status) {
        throw IllegalStateException("onProgressUpdate() must be overridden.")
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onProgressUpdate(T...)}.
     *
     * @param status The progress status
     */
    protected Unit onProgressUpdateInternal(final T status) {
        // empty by default
    }

    /**
     * Force a message to be shown in the dialog.
     * Call this only on the UI-Thread.
     *
     * @param message The message to show
     */
    protected Unit setMessage(final String message) {
        progress.setMessage(message)
    }

    /**
     * Launch the process in background.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    @SuppressWarnings("unchecked")
    override     protected Result doInBackground(final Params... params) {
        return doInBackgroundInternal(params)
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onProgressUpdate(T...)}.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    protected abstract Result doInBackgroundInternal(Params[] params)

    /**
     * Instead of a message set with cancelMessage parameter you may set an cancelListener to the progress dialog
     *
     * @param cancelListener
     */
    public Unit setOnCancelListener(final DialogInterface.OnClickListener cancelListener) {
        progress.setOnCancelListener(cancelListener)
    }
}
