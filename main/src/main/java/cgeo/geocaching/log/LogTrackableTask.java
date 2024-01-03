package cgeo.geocaching.log;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.storage.extension.LastTrackableAction;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;

import org.apache.commons.lang3.StringUtils;

public class LogTrackableTask extends AsyncTaskWithProgress<String, StatusCode> {

    private final LogTrackableActivity.LogTrackableTaskInterface taskInterface;
    private final Action1<StatusCode> onPostExecuteInternal;

    LogTrackableTask(final Activity activity, final String progressMessage, final LogTrackableActivity.LogTrackableTaskInterface taskInterface, final Action1<StatusCode> onPostExecuteInternal) {
        super(activity, null, progressMessage, true);
        this.taskInterface = taskInterface;
        this.onPostExecuteInternal = onPostExecuteInternal;
    }

    @Override
    protected StatusCode doInBackgroundInternal(final String[] params) {
        try {
            // Real call to post log
            final LogResult logResult = taskInterface.loggingManager.postLog(taskInterface.geocache, taskInterface.trackableLogEntry);

            if (logResult.isOk()) {
                LastTrackableAction.setAction(taskInterface.trackableLogEntry.geocode, taskInterface.trackableLogEntry.getAction());
            }
            // Display errors to the user
            if (StringUtils.isNotEmpty(logResult.getPostServerMessage())) {
                ActivityMixin.showToast(activity, logResult.getPostServerMessage());
            }

            // Return request status
            return logResult.getStatusCode();
        } catch (final RuntimeException e) {
            Log.e("LogTrackableActivity.Poster.doInBackgroundInternal", e);
        }
        return StatusCode.LOG_POST_ERROR;
    }

    @Override
    protected void onPostExecuteInternal(final StatusCode status) {
        onPostExecuteInternal.call(status);
    }

}
