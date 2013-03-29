package cgeo.geocaching.utils;

import cgeo.geocaching.activity.Progress;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * AsyncTask which automatically shows a progress dialog. Use it like the {@code AsyncTask} class, but leave away the
 * middle template parameter.
 *
 * @param <Params>
 * @param <Result>
 */
public abstract class AsyncTaskWithProgress<Params, Result> extends AsyncTask<Params, Integer, Result> {

    private final Progress progress = new Progress();
    private final Activity activity;
    private final int maxProgress;
    private final String progressTitle;
    private final String progressMessage;

    /**
     * Creates an AsyncTask with progress dialog, where the maximum is set to the given maxProgress.
     * 
     * @param activity
     * @param maxProgress
     * @param progressTitle
     * @param progressMessage
     */
    public AsyncTaskWithProgress(final Activity activity, final int maxProgress, final String progressTitle, final String progressMessage) {
        this.activity = activity;
        this.maxProgress = maxProgress;
        this.progressTitle = progressTitle;
        this.progressMessage = progressMessage;
    }

    /**
     * Creates an AsyncTask with progress dialog, where the maximum is set to indeterminate.
     * 
     * @param activity
     * @param progressTitle
     * @param progressMessage
     */
    public AsyncTaskWithProgress(final Activity activity, final String progressTitle, final String progressMessage) {
        this(activity, 0, progressTitle, progressMessage);
    }

    @Override
    protected void onPreExecute() {
        if (null != activity) {
            if (maxProgress <= 0) {
                progress.show(activity, progressTitle, progressMessage, true, null);
            }
            else {
                progress.show(activity, progressTitle, progressMessage, ProgressDialog.STYLE_HORIZONTAL, null);
            }
            progress.setMaxProgressAndReset(maxProgress);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        if (null != activity) {
            progress.dismiss();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... status) {
        if (null != activity) {
            progress.setProgress(status[0]);
        }
    }

    protected void setMessage(final String message) {
        progress.setMessage(message);
    }
}
