package cgeo.geocaching.log;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.twitter.Twitter;
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

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
        final String logMsg = params[0];
        try {
            // Set selected action
            final TrackableLog trackableLog = new TrackableLog(taskInterface.trackable.getGeocode(), taskInterface.trackable.getTrackingcode(), taskInterface.trackable.getName(), 0, 0, taskInterface.trackable.getBrand());
            trackableLog.setAction(taskInterface.typeSelected);
            // Real call to post log
            final LogResult logResult = taskInterface.loggingManager.postLog(taskInterface.geocache, trackableLog, taskInterface.date.getCalendar(), logMsg);

            // Now posting tweet if log is OK
            if (logResult.getPostLogResult() == StatusCode.NO_ERROR) {
                addLocalTrackableLog(logMsg);
                if (taskInterface.binding.tweet.isChecked() && taskInterface.binding.tweetBox.getVisibility() == View.VISIBLE) {
                    // TODO oldLogType as a temp workaround...
                    final LogEntry logNow = new LogEntry.Builder()
                            .setDate(taskInterface.date.getDate().getTime())
                            .setLogType(taskInterface.typeSelected.oldLogtype)
                            .setLog(logMsg)
                            .build();
                    Twitter.postTweetTrackable(taskInterface.trackable.getGeocode(), logNow);
                }
            }
            // Display errors to the user
            if (StringUtils.isNotEmpty(logResult.getLogId())) {
                ActivityMixin.showToast(activity, logResult.getLogId());
            }

            // Return request status
            return logResult.getPostLogResult();
        } catch (final RuntimeException e) {
            Log.e("LogTrackableActivity.Poster.doInBackgroundInternal", e);
        }
        return StatusCode.LOG_POST_ERROR;
    }

    @Override
    protected void onPostExecuteInternal(final StatusCode status) {
        onPostExecuteInternal.call(status);
    }

    /**
     * Adds the new log to the list of log entries for this trackable to be able to show it in the trackable
     * activity.
     */
    private void addLocalTrackableLog(final String logText) {
        // TODO create a LogTrackableEntry. For now use "oldLogtype" as a temporary migration path
        final LogEntry logEntry = new LogEntry.Builder()
                .setDate(taskInterface.date.getDate().getTime())
                .setLogType(taskInterface.typeSelected.oldLogtype)
                .setLog(logText)
                .build();
        final List<LogEntry> modifiedLogs = new ArrayList<>(taskInterface.trackable.getLogs());
        modifiedLogs.add(0, logEntry);
        taskInterface.trackable.setLogs(modifiedLogs);
        DataStore.saveTrackable(taskInterface.trackable);
    }
}
