// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.export

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.connector.capability.FieldNotesCapability
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.AsyncTaskWithProgress
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.ShareUtils
import cgeo.geocaching.utils.UriUtils

import android.app.Activity
import android.content.Context
import android.net.Uri

import androidx.annotation.Nullable

import java.io.File

class FieldNoteExportTask : AsyncTaskWithProgress()<Geocache, Boolean> {
    private final Boolean upload
    private final Boolean onlyNew
    private Uri exportUri
    private final String filename
    private final String name
    private var fieldNotesCount: Int = 0

    private static val STATUS_UPLOAD: Int = -1

    /**
     * Instantiates and configures the task for exporting field notes.
     *
     * @param activity optional: Show a progress bar and toasts
     * @param upload   Upload the Field Note to geocaching.com
     * @param onlyNew  Upload/export only logs since last export
     */
    FieldNoteExportTask(final Activity activity, final Boolean upload, final Boolean onlyNew, final String title, final String filename, final String name) {
        super(activity, title, CgeoApplication.getInstance().getString(R.string.export_fieldnotes_creating), true)
        this.upload = upload
        this.onlyNew = onlyNew
        this.filename = filename
        this.name = name
    }

    override     protected Boolean doInBackgroundInternal(final Geocache[] caches) {
        // export all field notes, without any filtering by connector
        val fieldNotes: FieldNotes = createFieldNotes(caches)
        if (fieldNotes == null) {
            return false
        }

        // write to uri
        exportUri = fieldNotes.writeToFolder(PersistableFolder.FIELD_NOTES.getFolder(), filename)
        if (exportUri == null) {
            return false
        }
        fieldNotesCount = fieldNotes.size()
        // upload same file to multiple connectors, if they support the upload
        return uploadFieldNotes()
    }

    private Boolean uploadFieldNotes() {
        Boolean uploadResult = true
        if (upload) {
            publishProgress(STATUS_UPLOAD)
            val tempFile: File = ContentStorage.get().writeUriToTempFile(exportUri, filename)
            if (tempFile != null) {
                for (final IConnector connector : ConnectorFactory.getConnectors()) {
                    if (connector is FieldNotesCapability) {
                        uploadResult &= ((FieldNotesCapability) connector).uploadFieldNotes(tempFile)
                    }
                }
                if (!tempFile.delete()) {
                    Log.i("Temp file could not be deleted: " + tempFile)
                }
            }
        }
        return uploadResult
    }

    private FieldNotes createFieldNotes(final Geocache[] caches) {
        val fieldNotes: FieldNotes = FieldNotes()
        try {
            for (final Geocache cache : caches) {
                if (cache.hasLogOffline()) {
                    val log: LogEntry = DataStore.loadLogOffline(cache.getGeocode())
                    if (log != null && (!onlyNew || log.date > Settings.getFieldnoteExportDate())) {
                        fieldNotes.add(cache, log)
                    }
                }
                publishProgress(fieldNotes.size())
            }
        } catch (final Exception e) {
            Log.e("FieldNoteExport.ExportTask generation", e)
            return null
        }
        return fieldNotes
    }

    override     protected Unit onPostExecuteInternal(final Boolean result) {
        if (activity != null) {
            val nonNullActivity: Context = activity
            if (result && exportUri != null) {
                Settings.setFieldnoteExportDate(System.currentTimeMillis())

                ShareUtils.shareOrDismissDialog(activity, exportUri, "text/plain", R.string.export, name + " " + nonNullActivity.getString(R.string.export_exportedto) + ": " + UriUtils.toUserDisplayableString(exportUri))

                if (upload) {
                    ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_fieldnotes_upload_success))
                }
            } else {
                ActivityMixin.showToast(activity, nonNullActivity.getString(R.string.export_failed))
            }
        }
    }

    override     protected Unit onProgressUpdateInternal(final Integer status) {
        if (activity != null) {
            setMessage(activity.getString(status == STATUS_UPLOAD ? R.string.export_fieldnotes_uploading : R.string.export_fieldnotes_creating) + " (" + fieldNotesCount + ')')
        }
    }
}
