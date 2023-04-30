package cgeo.geocaching.export;

import cgeo.geocaching.CgeoApplication;
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
import cgeo.geocaching.utils.AsyncTaskWithProgress;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.UriUtils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.File;

class FieldNoteExportTask extends AsyncTaskWithProgress<Geocache, Boolean> {
    private final boolean upload;
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
     * @param upload   Upload the Field Note to geocaching.com
     * @param onlyNew  Upload/export only new logs since last export
     */
    FieldNoteExportTask(@Nullable final Activity activity, final boolean upload, final boolean onlyNew, final String title, final String filename, final String name) {
        super(activity, title, CgeoApplication.getInstance().getString(R.string.export_fieldnotes_creating), true);
        this.upload = upload;
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
        // upload same file to multiple connectors, if they support the upload
        return uploadFieldNotes();
    }

    private Boolean uploadFieldNotes() {
        boolean uploadResult = true;
        if (upload) {
            publishProgress(STATUS_UPLOAD);
            final File tempFile = ContentStorage.get().writeUriToTempFile(exportUri, filename);
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
        }
        return uploadResult;
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
            final Context nonNullActivity = activity;
            if (result && exportUri != null) {
                Settings.setFieldnoteExportDate(System.currentTimeMillis());

                ShareUtils.shareOrDismissDialog(activity, exportUri, "text/plain", R.string.export, name + " " + nonNullActivity.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(exportUri));

                if (upload) {
                    ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_fieldnotes_upload_success));
                }
            } else {
                ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_failed));
            }
        }
    }

    @Override
    protected void onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(activity.getString(status == STATUS_UPLOAD ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + fieldNotesCount + ')');
        }
    }
}
