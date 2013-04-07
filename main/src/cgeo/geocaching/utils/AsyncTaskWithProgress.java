package cgeo.geocaching.utils;

import cgeo.geocaching.activity.Progress;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * AsyncTask which automatically shows a progress dialog. Use it like the {@code AsyncTask} class, but leave away the
 * middle template parameter. Override {@link #doInBackgroundInternal(Object[])} and related methods.
 * <p>
 * If no style is given, the progress dialog uses "determinate" style with known maximum. The progress maximum is
 * automatically derived from the number of {@code Params} given to the task in {@link #execute(Object...)}.
 * </p>
 * 
 * @param <Params>
 * @param <Result>
 */
public abstract class AsyncTaskWithProgress<Params, Result> extends AsyncTask<Params, Integer, Result> {

    private final Progress progress = new Progress();
    private final Activity activity;
    private final String progressTitle;
    private final String progressMessage;
    private boolean indeterminate = false;

    /**
     * Creates an AsyncTask with progress dialog.
     *
     * @param activity
     * @param progressTitle
     * @param progressMessage
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage) {
        this(activity, progressTitle, progressMessage, false);
    }

    /**
     * Creates an AsyncTask with progress dialog.
     *
     * @param activity
     * @param progressTitle
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle) {
        this(activity, progressTitle, null);
    }

    /**
     * Creates an AsyncTask with progress dialog.
     *
     * @param activity
     * @param progressTitle
     * @param progressMessage
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage, boolean indeterminate) {
        this.activity = activity;
        this.progressTitle = progressTitle;
        this.progressMessage = progressMessage;
        this.indeterminate = indeterminate;
    }

    /**
     * Creates an AsyncTask with progress dialog.
     *
     * @param activity
     * @param progressTitle
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, boolean indeterminate) {
        this(activity, progressTitle, null, indeterminate);
    }

    @Override
    protected final void onPreExecute() {
        if (null != activity) {
            if (indeterminate) {
                progress.show(activity, progressTitle, progressMessage, true, null);
            }
            else {
                progress.show(activity, progressTitle, progressMessage, ProgressDialog.STYLE_HORIZONTAL, null);
            }
        }
        onPreExecuteInternal();
    }

    /**
     * This method should typically be overridden by sub classes instead of {@link #onPreExecute()}.
     */
    protected void onPreExecuteInternal() {
        // empty by default
    }

    @Override
    protected final void onPostExecute(Result result) {
        onPostExecuteInternal(result);
        if (null != activity) {
            progress.dismiss();
        }
    }

    /**
     * This method should typically be overridden by sub classes instead of {@link #onPostExecute(Object)}.
     *
     * @param result
     */
    protected void onPostExecuteInternal(Result result) {
        // empty by default
    }

    @Override
    protected final void onProgressUpdate(Integer... status) {
        final int progressValue = status[0];
        if (null != activity && progressValue >= 0) {
            progress.setProgress(progressValue);
        }
        onProgressUpdateInternal(progressValue);
    }

    /**
     * This method should by overridden by sub classes instead of {@link #onProgressUpdate(Integer...)}.
     */
    protected void onProgressUpdateInternal(@SuppressWarnings("unused") int progress) {
        // empty by default
    }

    protected void setMessage(final String message) {
        progress.setMessage(message);
    }

    @Override
    protected final Result doInBackground(Params... params) {
        if (params != null) {
            progress.setMaxProgressAndReset(params.length);
        }
        return doInBackgroundInternal(params);
    }

    protected abstract Result doInBackgroundInternal(Params[] params);
}
