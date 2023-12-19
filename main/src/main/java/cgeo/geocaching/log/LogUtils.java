package cgeo.geocaching.log;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;
import cgeo.geocaching.utils.workertask.WorkerTaskLogic;

import android.content.Context;
import android.util.Pair;

import androidx.core.app.ComponentActivity;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class LogUtils {

    private LogUtils() {
        //no instance
    }

    public static boolean canDeleteLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsDeleteLog(logEntry);
    }

    public static boolean canEditLog(final Geocache cache, final LogEntry logEntry) {
        return cache != null && cache.supportsEditLog(logEntry);
    }

    public static void startEditLog(final Context activity, final Geocache cache, final LogEntry entry) {
        LogCacheActivity.startForEdit(activity, cache.getGeocode(), entry);
    }

    public static void deleteLog(final ComponentActivity activity, final Geocache cache, final LogEntry entry) {
        if (!canDeleteLog(cache, entry)) {
            ActivityMixin.showToast(activity, "Can't delete log");
            return;
        }
        SimpleDialog.ofContext(activity)
            .setTitle(TextParam.text("Delete log"))
            .setMessage(TextParam.text("Really delete log '" + entry.log + "' of cache '" + cache.getGeocode() + "'?"))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
            .confirm(() -> {
                WorkerTask.of(activity, LogUtils::deleteTaskLogic)
                    .addResultListener(result ->
                        SimpleDialog.ofContext(activity).setTitle(TextParam.text("DELETE RESULT: " + result.isOk())).show())
                    .addFeature(ProgressDialogFeature.of(activity, Void.class).setTitle("Deleting").setInitialMessage("Deleting log"))
                    .start(new Pair<>(cache, entry));
            });
    }

    private static WorkerTaskLogic<Pair<Geocache, LogEntry>, Void, LogResult> deleteTaskLogic() {
        return (Pair<Geocache, LogEntry> input, Consumer<Void> message, Supplier<Boolean> isCancelled) ->
            input.first.getLoggingManager().deleteLog(input.second);
    }


}
