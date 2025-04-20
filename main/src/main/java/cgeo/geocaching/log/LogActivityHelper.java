package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import android.annotation.TargetApi;

import androidx.core.app.ComponentActivity;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.tuple.ImmutableTriple;

/** Helper class to use in Activities which want to use cache/trackable logging functionality */
@TargetApi(24)
public class LogActivityHelper {

    private final ComponentActivity activity;

    private final WorkerTask<ImmutableTriple<Geocache, LogEntry, String>, String, LogResult> logDeleteTask;

    private final WorkerTask<ImmutableTriple<Geocache, LogEntry, LogEntry>, String, LogResult> logEditTask;

    private final WorkerTask<ImmutableTriple<Geocache, OfflineLogEntry, Map<String, Trackable>>, String, LogResult> logCreateTask;

    private final WorkerTask<ImmutableTriple<Geocache, TrackableLogEntry, TrackableConnector>, String, LogResult> logCreateTrackableTask;



    private BiConsumer<ResultType, LogResult> logResultConsumer;

    public enum ResultType { CREATE, EDIT, DELETE, CREATE_TRACKABLE }

    public LogActivityHelper(final ComponentActivity activity) {
        this.activity = activity;

        logCreateTask = WorkerTask.<ImmutableTriple<Geocache, OfflineLogEntry, Map<String, Trackable>>, String, LogResult>of(
                "log-create",
                (input, progress, cancelFlag) -> LogUtils.createLogTaskLogic(input.left, input.middle, input.right, progress),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.log_posting_log)))
            .observeResult(activity, result -> {
                if (logResultConsumer != null) {
                    logResultConsumer.accept(ResultType.CREATE, result);
                }
            }, null);

        logEditTask = WorkerTask.<ImmutableTriple<Geocache, LogEntry, LogEntry>, String, LogResult>of(
                "log-edit",
                (input, progress, cancelFlag) -> LogUtils.editLogTaskLogic(input.left, input.middle, input.right, progress),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.cache_log_menu_edit)))
            .observeResult(activity, result -> {
                if (logResultConsumer != null) {
                    logResultConsumer.accept(ResultType.EDIT, result);
                }
            }, null);

        logDeleteTask = WorkerTask.<ImmutableTriple<Geocache, LogEntry, String>, String, LogResult>of(
                "log-delete",
                (input, progress, cancelFlag) -> LogUtils.deleteLogTaskLogic(input.left, input.middle, input.right, progress),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.cache_log_menu_delete)))
            .observeResult(activity, result -> {
                if (logResultConsumer != null) {
                    logResultConsumer.accept(ResultType.DELETE, result);
                }
            }, null);

        logCreateTrackableTask = WorkerTask.<ImmutableTriple<Geocache, TrackableLogEntry, TrackableConnector>, String, LogResult>of(
                "log-create-trackable",
                (input, progress, cancelFlag) -> LogUtils.createLogTrackableTaskLogic(input.left, input.middle, input.right, progress),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.log_posting_log)))
            .observeResult(activity, result -> {
                if (logResultConsumer != null) {
                    logResultConsumer.accept(ResultType.CREATE_TRACKABLE, result);
                }
            }, null);

    }

    /** Set an optional consumer for the result of the various log actions */
    public LogActivityHelper setLogResultConsumer(final BiConsumer<ResultType, LogResult> consumer) {
        this.logResultConsumer = consumer;
        return this;
    }

    /** call this method when the parent activity finishes. It cleans up ressources */
    public void finish() {
        logDeleteTask.finish();
        logEditTask.finish();
        logCreateTask.finish();
    }

    /** create a log on the geocaching platform */
    public void createLog(final Geocache cache, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        logCreateTask.start(new ImmutableTriple<>(cache, logEntry, inventory));
    }

    /** edit a log on the geocaching platform */
    public void editLog(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry) {
        if (!LogUtils.canEditLog(cache, newEntry)) {
            //should never happen (would be a programming error), thus not translating message
            ActivityMixin.showToast(activity, "Can't edit log");
            return;
        }

        logEditTask.start(new ImmutableTriple<>(cache, oldEntry, newEntry));
    }


    /** delete a log on the geocaching platform */
    public void deleteLog(final Geocache cache, final LogEntry entry) {
        if (!LogUtils.canDeleteLog(cache, entry)) {
            //should never happen (would be a programming error), thus not translating message
            ActivityMixin.showToast(activity, "Can't delete log");
            return;
        }
        final SimpleDialog dialog = SimpleDialog.ofContext(activity)
            .setTitle(TextParam.id(R.string.cache_log_menu_delete))
            .setMessage(TextParam.id(R.string.log_delete_confirm,
                entry.logType.getL10n(), entry.author, Formatter.formatShortDateVerbally(entry.date)))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO);
        if (entry.isOwn()) {
            dialog.confirm(() -> logDeleteTask.start(new ImmutableTriple<>(cache, entry, null)));
        } else {
            dialog.input(new SimpleDialog.InputOptions().setLabel("Reason"), reasonText -> logDeleteTask.start(new ImmutableTriple<>(cache, entry, reasonText)));
        }
    }

    /** create a trackable log on the trackable platform */
    public void createLogTrackable(final Geocache cache, final TrackableLogEntry logEntry, final TrackableConnector connector) {
        logCreateTrackableTask.start(new ImmutableTriple<>(cache, logEntry, connector));
    }


}
