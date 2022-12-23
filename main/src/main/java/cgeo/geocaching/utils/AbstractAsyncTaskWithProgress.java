package cgeo.geocaching.utils;

import cgeo.geocaching.activity.Progress;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Abstract AsyncTask which automatically shows a progress dialog. Use it like the {@code AsyncTask} class, but leave away the
 * middle template parameter. Override {@link #doInBackgroundInternal(Object[])} and related methods.
 *
 * Do not use this class directly, instead use either AsyncTaskWithProgress or AsyncTaskWithProgressText.
 */
public abstract class AbstractAsyncTaskWithProgress<Params, T, Result> extends AsyncTask<Params, T, Result> {

    @NonNull protected final Progress progress = new Progress();
    @Nullable protected final Activity activity;
    protected final String progressTitle;
    protected final String progressMessage;

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AbstractAsyncTaskWithProgress(@Nullable final Activity activity, final String progressTitle, final String progressMessage) {
        this.activity = activity;
        this.progressTitle = progressTitle;
        this.progressMessage = progressMessage;
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AbstractAsyncTaskWithProgress(@Nullable final Activity activity, final String progressTitle) {
        this(activity, progressTitle, null);
    }

    /**
     * Show the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        if (activity != null) {
            progress.show(activity, progressTitle, progressMessage, true, null);
        }
        onPreExecuteInternal();
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onPreExecute()}.
     */
    @SuppressWarnings("EmptyMethod")
    protected void onPreExecuteInternal() {
        // empty by default
    }

    /**
     * Hide the progress dialog.
     * This method won't be invoked if the task was cancelled.
     *
     * @param result The result of the operation computed by doInBackground(Params...).
     */
    @Override
    protected final void onPostExecute(final Result result) {
        onPostExecuteInternal(result);
        if (progress.isShowing()) {
            progress.dismiss();
        }
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onPostExecute(Object)}.
     *
     * @param result The result of the operation computed by {@link #doInBackground(Object...)}.
     */
    protected void onPostExecuteInternal(final Result result) {
        // empty by default
    }

    /**
     * Subclasses must implement the logic for the progress updates.
     *
     * @param status The new progress status
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void onProgressUpdate(final T... status) {
        throw new IllegalStateException("onProgressUpdate() must be overridden.");
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onProgressUpdate(T...)}.
     *
     * @param status The new progress status
     */
    protected void onProgressUpdateInternal(final T status) {
        // empty by default
    }

    /**
     * Force a new message to be shown in the dialog.
     * Call this only on the UI-Thread.
     *
     * @param message The new message to show
     */
    protected void setMessage(final String message) {
        progress.setMessage(message);
    }

    /**
     * Launch the process in background.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Result doInBackground(final Params... params) {
        return doInBackgroundInternal(params);
    }

    /**
     * This method should typically be overridden by final sub classes instead of {@link #onProgressUpdate(T...)}.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    protected abstract Result doInBackgroundInternal(Params[] params);

    /**
     * Instead of a message set with cancelMessage parameter you may set an cancelListener to the progress dialog
     *
     * @param cancelListener
     */
    public void setOnCancelListener(final DialogInterface.OnClickListener cancelListener) {
        progress.setOnCancelListener(cancelListener);
    }
}
