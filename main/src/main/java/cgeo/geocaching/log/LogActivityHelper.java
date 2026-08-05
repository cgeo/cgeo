package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.service.LogPostingService;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import androidx.activity.ComponentActivity;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.tuple.ImmutableTriple;

/** Helper class to use in Activities which want to use cache/trackable logging functionality */
public class LogActivityHelper {

    private final ComponentActivity activity;

    private final WorkerTask<ImmutableTriple<Geocache, LogEntry, String>, String, LogResult> logDeleteTask;

    private final WorkerTask<ImmutableTriple<Geocache, TrackableLogEntry, TrackableConnector>, String, LogResult> logCreateTrackableTask;

    private static final int MAX_ALLOWED_CHARS_DELETE_REASON = 125;

    private BiConsumer<ResultType, LogResult> logResultConsumer;

    public enum ResultType { CREATE, EDIT, DELETE, CREATE_TRACKABLE }

    public LogActivityHelper(final ComponentActivity activity) {
        this.activity = activity;

        logDeleteTask = WorkerTask.<ImmutableTriple<Geocache, LogEntry, String>, String, LogResult>of(
                "log-delete",
                (input, progress, cancelFlag) -> LogUtils.deleteLogTaskLogic(input.left, input.middle, input.right, progress),
                cgeo.geocaching.utils.AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.cache_log_menu_delete)))
            .observeResult(activity, result -> {
                if (logResultConsumer != null) {
                    logResultConsumer.accept(ResultType.DELETE, result);
                }
            }, null);

        logCreateTrackableTask = WorkerTask.<ImmutableTriple<Geocache, TrackableLogEntry, TrackableConnector>, String, LogResult>of(
                "log-create-trackable",
                (input, progress, cancelFlag) -> LogUtils.createLogTrackableTaskLogic(input.left, input.middle, input.right, progress),
                cgeo.geocaching.utils.AndroidRxUtils.networkScheduler)
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

    /** call this method when the parent activity finishes. It cleans up resources */
    public void finish() {
        logDeleteTask.finish();
    }

    /** create a log on the geocaching platform */
    public void createLog(final Geocache cache, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        LogPostingService.startCreate(activity, cache, logEntry, inventory);
    }

    /** edit a log on the geocaching platform */
    public void editLog(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry) {
        if (!LogUtils.canEditLog(cache, newEntry)) {
            //should never happen (would be a programming error), thus not translating message
            ActivityMixin.showToast(activity, "Can't edit log");
            return;
        }
        LogPostingService.startEdit(activity, cache, oldEntry, newEntry);
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
        if (LogUtils.isOwnLog(entry, cache)) {
            dialog.confirm(() -> logDeleteTask.start(new ImmutableTriple<>(cache, entry, null)));
        } else {
            dialog.input(new SimpleDialog.InputOptions().setMaxAllowedLength(MAX_ALLOWED_CHARS_DELETE_REASON), reasonText -> logDeleteTask.start(new ImmutableTriple<>(cache, entry, reasonText)));
        }
    }

    /** create a trackable log on the trackable platform */
    public void createLogTrackable(final Geocache cache, final TrackableLogEntry logEntry, final TrackableConnector connector) {
        logCreateTrackableTask.start(new ImmutableTriple<>(cache, logEntry, connector));
    }
}
