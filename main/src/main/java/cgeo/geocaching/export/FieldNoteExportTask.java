package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.capability.FieldNotesCapability;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;
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

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Exports (and optionally uploads) field notes using a {@link WorkerTask}, so a running export survives configuration
 * changes (e.g. rotation) of the activity which started it.
 * <br>
 * Activities which want to start a field note export must call {@link #attach(ComponentActivity)} once per instance,
 * before they are started (e.g. in a field initializer or in onCreate).
 */
public final class FieldNoteExportTask {

    private static final String TASK_ID = "fieldnote-export";

    /** Parameters of one export run */
    static class Input {
        @NonNull private final Geocache[] caches;
        @NonNull private final List<FieldNotesCapability> uploadTargets;
        private final boolean onlyNew;
        @NonNull private final String filename;
        @NonNull private final String name;

        /**
         * @param uploadTargets connectors to upload the field notes to (may be empty)
         * @param onlyNew       Upload/export only new logs since last export
         */
        Input(@NonNull final Geocache[] caches, @NonNull final List<FieldNotesCapability> uploadTargets, final boolean onlyNew, @NonNull final String filename, @NonNull final String name) {
            this.caches = caches;
            this.uploadTargets = uploadTargets;
            this.onlyNew = onlyNew;
            this.filename = filename;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return filename + "(" + caches.length + " caches, upload to " + uploadTargets.size() + " sites, onlyNew=" + onlyNew + ")";
        }
    }

    /** Progress of one export run: whether the upload has started, and the number of field notes so far */
    private static class ExportProgress {
        private final boolean uploading;
        private final int count;

        private ExportProgress(final boolean uploading, final int count) {
            this.uploading = uploading;
            this.count = count;
        }
    }

    /** Result of one export run: the written file (null if writing failed) and the result of each upload */
    private static class ExportResult {
        @NonNull private final String name;
        @Nullable private Uri uri;
        @NonNull private final List<String> uploadedTo = new ArrayList<>();
        @NonNull private final List<String> uploadFailedTo = new ArrayList<>();

        private ExportResult(@NonNull final String name) {
            this.name = name;
        }
    }

    private FieldNoteExportTask() {
        // utility class
    }

    /** Attaches the field note export task to the given activity: shows progress and handles the result while the activity is active */
    public static void attach(@NonNull final ComponentActivity activity) {
        WorkerTask.<Input, ExportProgress, ExportResult>of(
                TASK_ID,
                (input, progress, cancelFlag) -> {
                    final ExportResult result = export(input, progress);
                    if (result.uri != null) {
                        // done here rather than in the result handler, which doesn't run if the activity is gone by then
                        Settings.setFieldnoteExportDate(System.currentTimeMillis());
                    }
                    return result;
                },
                AndroidRxUtils.computationScheduler)
            .addFeature(ProgressDialogFeature.<ExportProgress>of(activity)
                .setTitle(new FieldNoteExport().getProgressTitle())
                .setInitialMessage(LocalizationUtils.getString(R.string.export_fieldnotes_creating))
                .setMessageMapper(p -> LocalizationUtils.getString(p.uploading ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + p.count + ')')
                .setProgressIndeterminate())
            .observeResult(activity, result -> onExportFinished(activity, result), error -> onExportFailed(activity));
    }

    /**
     * Starts a field note export. If no activity is given (or the activity did not call {@link #attach(ComponentActivity)}),
     * the export runs in background without progress dialog and result message.
     */
    static void start(@Nullable final Activity activity, @NonNull final Input input) {
        final WorkerTask<Input, ExportProgress, ExportResult> task = activity == null ? null : WorkerTask.get(TASK_ID);
        if (task != null) {
            task.start(input);
            return;
        }
        if (activity != null) {
            Log.w("FieldNoteExportTask: activity did not attach to export task, exporting without feedback: " + activity.getClass().getName());
        }
        AndroidRxUtils.computationScheduler.scheduleDirect(() -> export(input, null));
    }

    /**
     * Writes the field note file and uploads it to the selected sites, synchronously.
     *
     * @return the result: the Uri of the written file (null if writing failed) and the result of each upload
     */
    @WorkerThread
    @NonNull
    private static ExportResult export(@NonNull final Input input, @Nullable final Consumer<ExportProgress> progress) {
        final ExportResult result = new ExportResult(input.name);
        // export all field notes, without any filtering by connector
        final FieldNotes fieldNotes = createFieldNotes(input, progress);
        if (fieldNotes == null) {
            return result;
        }

        // write to uri
        result.uri = fieldNotes.writeToFolder(PersistableFolder.FIELD_NOTES.getFolder(), input.filename);
        if (result.uri == null) {
            return result;
        }

        if (!input.uploadTargets.isEmpty()) {
            if (progress != null) {
                progress.accept(new ExportProgress(true, fieldNotes.size()));
            }
            // upload the same (hybrid) file to all selected connectors; the export itself has succeeded
            // at this point, the result of each upload is reported per site
            uploadFieldNotes(result.uri, input, result);
        }
        return result;
    }

    private static void uploadFieldNotes(@NonNull final Uri uri, @NonNull final Input input, @NonNull final ExportResult result) {
        final File tempFile = ContentStorage.get().writeUriToTempFile(uri, input.filename);
        for (final FieldNotesCapability connector : input.uploadTargets) {
            // a site rejects e.g. a file without any record of its own platform; that must not
            // turn the uploads to the other sites into a failure
            if (tempFile != null && connector.uploadFieldNotes(tempFile)) {
                result.uploadedTo.add(connector.getName());
            } else {
                result.uploadFailedTo.add(connector.getName());
            }
        }
        if (tempFile != null && !tempFile.delete()) {
            Log.i("Temp file could not be deleted: " + tempFile);
        }
    }

    @Nullable
    private static FieldNotes createFieldNotes(@NonNull final Input input, @Nullable final Consumer<ExportProgress> progress) {
        final FieldNotes fieldNotes = new FieldNotes();
        try {
            for (final Geocache cache : input.caches) {
                if (cache.hasLogOffline()) {
                    final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
                    if (log != null && (!input.onlyNew || log.date > Settings.getFieldnoteExportDate())) {
                        fieldNotes.add(cache, log);
                    }
                }
                if (progress != null) {
                    progress.accept(new ExportProgress(false, fieldNotes.size()));
                }
            }
        } catch (final Exception e) {
            Log.e("FieldNoteExport.ExportTask generation", e);
            return null;
        }
        return fieldNotes;
    }

    private static void onExportFinished(@NonNull final Activity activity, @NonNull final ExportResult result) {
        if (result.uri == null) {
            onExportFailed(activity);
            return;
        }
        ShareUtils.shareOrDismissDialog(activity, result.uri, "text/plain", R.string.export, result.name + " " + LocalizationUtils.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(result.uri));
        for (final String site : result.uploadedTo) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_fieldnotes_upload_to_success, site));
        }
        for (final String site : result.uploadFailedTo) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_fieldnotes_upload_to_failed, site));
        }
    }

    private static void onExportFailed(@NonNull final Activity activity) {
        ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
    }
}
