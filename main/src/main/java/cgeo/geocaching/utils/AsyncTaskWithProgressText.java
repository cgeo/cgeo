package cgeo.geocaching.utils;

import android.app.Activity;

import org.apache.commons.lang3.StringUtils;

/**
 * AsyncTask which automatically shows a progress dialog. The progress is tracked with text messages.
 *
 * Use it like the {@code AsyncTask} class, but leave away the middle template parameter. Override
 * {@link #doInBackgroundInternal(Object[])} and related methods.
 *
 * <p>
 * Use {@code publishProgress(String)} to change the text message.
 * </p>
 */
public abstract class AsyncTaskWithProgressText<Params, Result> extends AbstractAsyncTaskWithProgress<Params, String, Result> {

    /**
     * Creates an AsyncTask with progress dialog.
     */
    public AsyncTaskWithProgressText(final Activity activity, final String progressTitle, final String progressMessage) {
        super(activity, progressTitle, progressMessage);
    }

    /**
     * Define the progress logic.
     *
     * @param status The new progress status
     */
    @Override
    protected final void onProgressUpdate(final String... status) {
        final String progressValue = status[0];
        if (activity != null && StringUtils.isNotBlank(progressValue)) {
            progress.setMessage(progressValue);
        }
        onProgressUpdateInternal(progressValue);
    }
}
