package cgeo.geocaching.log;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import android.annotation.TargetApi;
import android.util.Pair;

import androidx.core.app.ComponentActivity;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.tuple.ImmutableTriple;

@TargetApi(24)
public class LogActivityHelper {

    private final ComponentActivity activity;

    private final WorkerTask<Pair<Geocache, LogEntry>, Void, LogResult> logDeleteTask;

    private final WorkerTask<ImmutableTriple<Geocache, LogEntry, LogEntry>, String, LogResult> logEditTask;

    private Consumer<LogResult> logEditResultConsumer;

    public LogActivityHelper(final ComponentActivity activity) {
        this.activity = activity;

        logEditTask = WorkerTask.<ImmutableTriple<Geocache, LogEntry, LogEntry>, String, LogResult>of("log-edit",
            input -> Observable.create(emit -> {
               final LogResult result = LogUtils.editLogTaskLogic(input.left, input.middle, input.right, p -> emit.onNext(WorkerTask.TaskValue.progress(p)));
               emit.onNext(WorkerTask.TaskValue.result(result));
            }))
            .addFeature(ProgressDialogFeature.of(activity).setTitle("Editing").setInitialMessage("Editing log"))
            .observeResult(activity, result ->
                SimpleDialog.ofContext(activity).setTitle(TextParam.text("Edit result: " + result.isOk()))
                .setMessage(TextParam.text("Edit result: " + result)).show(() -> {
                    if (logEditResultConsumer != null) {
                        logEditResultConsumer.accept(result);
                    }
                }));

        logDeleteTask = WorkerTask.<Pair<Geocache, LogEntry>, Void, LogResult>of("log-delete",
            input -> Observable.create(emit -> {
                emit.onNext(WorkerTask.TaskValue.result(LogUtils.deleteLogTaskLogic(input.first, input.second)));
            }))
            .addFeature(ProgressDialogFeature.of(activity).setTitle("Deleting").setInitialMessage("Deleting log"))
            .observeResult(activity, result ->
                SimpleDialog.ofContext(activity).setTitle(TextParam.text("Delete result: " + result.isOk()))
                .setMessage(TextParam.text("Delete result: " + result)).show());

    }

    public LogActivityHelper setLogEditResultConsumer(final Consumer<LogResult> consumer) {
        this.logEditResultConsumer = consumer;
        return this;
    }

    public void finish() {
        logDeleteTask.finish();
        logEditTask.finish();
    }

    public void editLog(final Geocache cache, final LogEntry oldEntry, final LogEntry newEntry) {
        if (!LogUtils.canEditLog(cache, newEntry)) {
            ActivityMixin.showToast(activity, "Can't edit log");
            return;
        }

        logEditTask.start(new ImmutableTriple<>(cache, oldEntry, newEntry));
    }


    public void deleteLog(final Geocache cache, final LogEntry entry) {
        if (!LogUtils.canDeleteLog(cache, entry)) {
            ActivityMixin.showToast(activity, "Can't delete log");
            return;
        }
        SimpleDialog.ofContext(activity)
            .setTitle(TextParam.text("Delete log"))
            .setMessage(TextParam.text("Really delete log '" + entry.log + "' of cache '" + cache.getGeocode() + "'?"))
            .setButtons(SimpleDialog.ButtonTextSet.YES_NO)
            .confirm(() -> logDeleteTask.start(new Pair<>(cache, entry)));
    }


}
