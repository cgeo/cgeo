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
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FieldNoteExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
    private final List<FieldNotesCapability> uploadTargets;
    private final List<String> uploadedTo = new ArrayList<>();
    private final List<String> uploadFailedTo = new ArrayList<>();
    private final boolean onlyNew;
    private Uri exportUri;
    private final String filename;
    private final String name;
    private int fieldNotesCount = 0;

    private static final int STATUS_UPLOAD = -1;

    /**
     * Instantiates and configures the task for exporting field notes.
     *
     * @param activity optional: Show a progress bar and toasts
     * @param uploadTargets connectors to upload the field notes to (may be empty)
     * @param onlyNew  Upload/export only new logs since last export
     */
    FieldNoteExportTask(@Nullable final Activity activity, final List<FieldNotesCapability> uploadTargets, final boolean onlyNew, final String title, final String filename, final String name) {
        super(activity, title, LocalizationUtils.getString(R.string.export_fieldnotes_creating), true);
        this.uploadTargets = uploadTargets;
        this.onlyNew = onlyNew;
        this.filename = filename;
        this.name = name;
    }

    @Override
    protected Boolean doInBackgroundInternal(final Geocache[] caches) {
        // export all field notes, without any filtering by connector
        final FieldNotes fieldNotes = createFieldNotes(caches);
        if (fieldNotes == null) {
            return false;
        }

        // write to uri
        exportUri = fieldNotes.writeToFolder(PersistableFolder.FIELD_NOTES.getFolder(), filename);
        if (exportUri == null) {
            return false;
        }
        fieldNotesCount = fieldNotes.size();
        // upload the same (hybrid) file to all selected connectors; the export itself has succeeded
        // at this point, the result of each upload is reported per site
        uploadFieldNotes();
        return true;
    }

    private void uploadFieldNotes() {
        if (uploadTargets.isEmpty()) {
            return;
        }
        publishProgress(STATUS_UPLOAD);
        final File tempFile = ContentStorage.get().writeUriToTempFile(exportUri, filename);
        for (final FieldNotesCapability connector : uploadTargets) {
            // a site rejects e.g. a file without any record of its own platform; that must not
            // turn the uploads to the other sites into a failure
            if (tempFile != null && connector.uploadFieldNotes(tempFile)) {
                uploadedTo.add(connector.getName());
            } else {
                uploadFailedTo.add(connector.getName());
            }
        }
        if (tempFile != null && !tempFile.delete()) {
            Log.i("Temp file could not be deleted: " + tempFile);
        }
    }

    @Nullable
    private FieldNotes createFieldNotes(final Geocache[] caches) {
        final FieldNotes fieldNotes = new FieldNotes();
        try {
            for (final Geocache cache : caches) {
                if (cache.hasLogOffline()) {
                    final LogEntry log = DataStore.loadLogOffline(cache.getGeocode());
                    if (log != null && (!onlyNew || log.date > Settings.getFieldnoteExportDate())) {
                        fieldNotes.add(cache, log);
                    }
                }
                publishProgress(fieldNotes.size());
            }
        } catch (final Exception e) {
            Log.e("FieldNoteExport.ExportTask generation", e);
            return null;
        }
        return fieldNotes;
    }

    @Override
    protected void onPostExecuteInternal(final Boolean result) {
        if (activity != null) {
            if (result && exportUri != null) {
                Settings.setFieldnoteExportDate(System.currentTimeMillis());

                ShareUtils.shareOrDismissDialog(activity, exportUri, "text/plain", R.string.export, name + " " + LocalizationUtils.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(exportUri));

                for (final String site : uploadedTo) {
                    ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_fieldnotes_upload_to_success, site));
                }
                for (final String site : uploadFailedTo) {
                    ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_fieldnotes_upload_to_failed, site));
                }
            } else {
                ActivityMixin.showToast(activity, LocalizationUtils.getString(R.string.export_failed));
            }
        }
    }

    @Override
    protected void onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(LocalizationUtils.getString(status == STATUS_UPLOAD ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + fieldNotesCount + ')');
        }
    }
}
