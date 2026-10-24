package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import android.app.Activity;
import android.net.Uri;
import android.util.Pair;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

/**
 * Exports geocaches to a GPX file using a {@link WorkerTask}, so a running export survives configuration changes
 * (e.g. rotation) of the activity which started it.
 * <br>
 * Activities which want to start a GPX export must call {@link #attach(ComponentActivity)} once per instance,
 * before they are started (e.g. in a field initializer or in onCreate).
 */
public final class GpxExportTask {

    private static final String TASK_ID = "gpx-export";

    /** Parameters of one export run */
    public static class Input {
        @NonNull private final String[] geocodes;
        @NonNull private final String filename;
        @NonNull private final String name;

        public Input(@NonNull final String[] geocodes, @NonNull final String filename, @NonNull final String name) {
            this.geocodes = geocodes;
            this.filename = filename;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return filename + "(" + geocodes.length + " caches)";
        }
    }

    /** Progress of one export run: number of exported caches and total number of caches */
    private static class ExportProgress {
        private final int exported;
        private final int total;

        private ExportProgress(final int exported, final int total) {
            this.exported = exported;
            this.total = total;
        }
    }

    private GpxExportTask() {
        // utility class
    }

    /** Attaches the GPX export task to the given activity: shows progress and handles the result while the activity is active */
    public static void attach(@NonNull final ComponentActivity activity) {
        WorkerTask.<Input, ExportProgress, Pair<String, Uri>>of(
                TASK_ID,
                (input, progress, cancelFlag) -> new Pair<>(input.name, export(input, progress)),
                AndroidRxUtils.computationScheduler)
            .addFeature(ProgressDialogFeature.<ExportProgress>of(activity)
                .setTitle(new GpxExport().getProgressTitle())
                .setMessageMapper(p -> LocalizationUtils.getPlural(R.plurals.cache_counts, p.total))
                .setProgressParameter(100, p -> p.total == 0 ? 0 : p.exported * 100 / p.total))
            .observeResult(activity, result -> onExportFinished(activity, result.first, result.second),
                error -> onExportFinished(activity, null, null));
    }

    /**
     * Starts a GPX export. If no activity is given (or the activity did not call {@link #attach(ComponentActivity)}),
     * the export runs in background without progress dialog and result message.
     */
    static void start(@Nullable final Activity activity, @NonNull final Input input) {
        final WorkerTask<Input, ExportProgress, Pair<String, Uri>> task = activity == null ? null : WorkerTask.get(TASK_ID);
        if (task != null) {
            task.start(input);
            return;
        }
        if (activity != null) {
            Log.w("GpxExportTask: activity did not attach to export task, exporting without feedback: " + activity.getClass().getName());
        }
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> export(input, null));
    }

    /**
     * Writes the GPX file synchronously.
     *
     * @return the Uri of the written file, or null if the export failed
     */
    @WorkerThread
    @Nullable
    static Uri export(@NonNull final Input input, @Nullable final Consumer<ExportProgress> progress) {
        final int total = input.geocodes.length;
        if (progress != null) {
            progress.accept(new ExportProgress(0, total));
        }

        final Uri uri = ContentStorage.get().create(PersistableFolder.GPX, input.filename);
        if (uri == null) {
            return null;
        }

        BufferedWriter writer = null;
        try (OutputStream os = ContentStorage.get().openForWrite(uri)) {
            if (os == null) {
                return null;
            }

            writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
            new GpxSerializer().writeGPX(Arrays.asList(input.geocodes), writer, countExported -> {
                if (progress != null) {
                    progress.accept(new ExportProgress(countExported, total));
                }
            });
        } catch (final IOException e) {
            Log.e("GpxExport.ExportTask export", e);
            // delete partial GPX file on error
            ContentStorage.get().delete(uri);

            return null;
        } finally {
            IOUtils.closeQuietly(writer);
        }

        return uri;
    }

    private static void onExportFinished(@NonNull final Activity activity, @Nullable final String name, @Nullable final Uri uri) {
        if (uri != null) {
            ShareUtils.shareOrDismissDialog(activity, uri, "application/xml", R.string.export, name + ' ' + LocalizationUtils.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(uri));
        } else {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
        }
    }
}
