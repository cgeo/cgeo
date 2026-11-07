package cgeo.geocaching.export;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
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
        private final boolean upload;
        private final boolean onlyNew;
        @NonNull private final String filename;
        @NonNull private final String name;

        /**
         * @param upload  Upload the Field Note to geocaching.com
         * @param onlyNew Upload/export only new logs since last export
         */
        Input(@NonNull final Geocache[] caches, final boolean upload, final boolean onlyNew, @NonNull final String filename, @NonNull final String name) {
            this.caches = caches;
            this.upload = upload;
            this.onlyNew = onlyNew;
            this.filename = filename;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return filename + "(" + caches.length + " caches, upload=" + upload + ", onlyNew=" + onlyNew + ")";
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

    /** Result of one export run */
    private static class ExportResult {
        @NonNull private final String name;
        private final boolean upload;
        @Nullable private final Uri uri;

        private ExportResult(@NonNull final String name, final boolean upload, @Nullable final Uri uri) {
            this.name = name;
            this.upload = upload;
            this.uri = uri;
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
                    final Uri uri = export(input, progress);
                    if (uri != null) {
                        // done here rather than in the result handler, which doesn't run if the activity is gone by then
                        Settings.setFieldnoteExportDate(System.currentTimeMillis());
                    }
                    return new ExportResult(input.name, input.upload, uri);
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
     * Writes the field note file and uploads it if requested, synchronously.
     *
     * @return the Uri of the written file, or null if writing or uploading failed
     */
    @WorkerThread
    @Nullable
    private static Uri export(@NonNull final Input input, @Nullable final Consumer<ExportProgress> progress) {
        // export all field notes, without any filtering by connector
        final FieldNotes fieldNotes = createFieldNotes(input, progress);
        if (fieldNotes == null) {
            return null;
        }

        // write to uri
        final Uri uri = fieldNotes.writeToFolder(PersistableFolder.FIELD_NOTES.getFolder(), input.filename);
        if (uri == null) {
            return null;
        }

        if (input.upload) {
            if (progress != null) {
                progress.accept(new ExportProgress(true, fieldNotes.size()));
            }
            // upload same file to multiple connectors, if they support the upload
            if (!uploadFieldNotes(uri, input.filename)) {
                return null;
            }
        }
        return uri;
    }

    private static boolean uploadFieldNotes(@NonNull final Uri uri, @NonNull final String filename) {
        boolean uploadResult = true;
        final File tempFile = ContentStorage.get().writeUriToTempFile(uri, filename);
        if (tempFile != null) {
            for (final IConnector connector : ConnectorFactory.getConnectors()) {
                if (connector instanceof FieldNotesCapability) {
                    uploadResult &= ((FieldNotesCapability) connector).uploadFieldNotes(tempFile);
                }
            }
            if (!tempFile.delete()) {
                Log.i("Temp file could not be deleted: " + tempFile);
            }
        }
        return uploadResult;
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
        if (result.upload) {
            ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_fieldnotes_upload_success));
        }
    }

    private static void onExportFailed(@NonNull final Activity activity) {
        ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
    }
}
