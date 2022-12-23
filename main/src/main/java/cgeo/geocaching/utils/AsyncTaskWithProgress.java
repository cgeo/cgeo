package cgeo.geocaching.utils;

import android.app.Activity;
import android.app.ProgressDialog;

/**
 * AsyncTask which automatically shows a progress dialog. The progress is tracked with integers.
 *
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
public abstract class AsyncTaskWithProgress<Params, Result> extends AbstractAsyncTaskWithProgress<Params, Integer, Result> {

    private boolean indeterminate = false;

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage) {
        this(activity, progressTitle, progressMessage, false);
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle) {
        this(activity, progressTitle, null);
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage, final boolean indeterminate) {
        super(activity, progressTitle, progressMessage);
        this.indeterminate = indeterminate;
    }

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final boolean indeterminate) {
        this(activity, progressTitle, null, indeterminate);
    }

    /**
     * Show the progress dialog.
     */
    @Override
    protected final void onPreExecute() {
        if (activity != null) {
            if (indeterminate) {
                progress.show(activity, progressTitle, progressMessage, true, null);
            } else {
                progress.show(activity, progressTitle, progressMessage, ProgressDialog.STYLE_HORIZONTAL, null);
            }
        }
        onPreExecuteInternal();
    }

    /**
     * Define the progress logic.
     *
     * @param status The new progress status
     */
    @Override
    protected final void onProgressUpdate(final Integer... status) {
        final int progressValue = status[0];
        if (activity != null && progressValue >= 0) {
            progress.setProgress(progressValue);
        }
        onProgressUpdateInternal(progressValue);
    }

    /**
     * Launch the process in background.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final Result doInBackground(final Params... params) {
        if (params != null) {
            progress.setMaxProgressAndReset(params.length);
        }
        return doInBackgroundInternal(params);
    }
}
