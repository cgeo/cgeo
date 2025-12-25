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

package cgeo.geocaching.files

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.Progress
import cgeo.geocaching.models.GCList
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.File
import java.lang.ref.WeakReference
import java.util.Arrays
import java.util.List

import org.apache.commons.lang3.StringUtils

class GPXImporter {
    static val IMPORT_STEP_START: Int = 0
    static val IMPORT_STEP_READ_FILE: Int = 1
    static val IMPORT_STEP_READ_WPT_FILE: Int = 2
    static val IMPORT_STEP_FINISHED: Int = 5
    static val IMPORT_STEP_FINISHED_WITH_ERROR: Int = 6
    static val IMPORT_STEP_CANCEL: Int = 7
    static val IMPORT_STEP_CANCELED: Int = 8
    static val IMPORT_STEP_STATIC_MAPS_SKIPPED: Int = 9

    public static val WAYPOINTS_FILE_SUFFIX: String = "-wpts"
    public static val WAYPOINTS_FILE_SUFFIX_AND_EXTENSION: String = WAYPOINTS_FILE_SUFFIX + FileUtils.GPX_FILE_EXTENSION

    private static val GPX_MIME_TYPES: List<String> = Arrays.asList("text/xml", "application/xml")
    private static val ZIP_MIME_TYPES: List<String> = Arrays.asList("application/zip", "application/x-compressed", "application/x-zip-compressed", "application/x-zip", "application/octet-stream")

    private val progress: Progress = Progress(true)

    private final Int listId
    private final Activity fromActivity
    private final Handler importFinishedHandler
    private val progressHandler: DisposableHandler = ProgressHandler(progress)
    private final Handler importStepHandler

    public GPXImporter(final Activity fromActivity, final Int listId, final Handler importFinishedHandler) {
        this.listId = listId
        this.fromActivity = fromActivity
        this.importFinishedHandler = importFinishedHandler
        importStepHandler = ImportStepHandler(this, fromActivity)
    }

    /**
     * Import GPX file from URI.
     *
     * @param uri URI of the file to import
     */
    public Unit importGPX(final Uri uri, final String mimeType, final String pathName) {
        val contentResolver: ContentResolver = fromActivity.getContentResolver()

        Log.i("importGPX: " + uri + ", mimetype=" + mimeType)

        FileType fileType = FileTypeDetector(uri, contentResolver)
                .getFileType()

        if (fileType == FileType.UNKNOWN) {
            fileType = getFileTypeFromPathName(pathName)
        }
        if (fileType == FileType.UNKNOWN) {
            fileType = getFileTypeFromMimeType(mimeType)
        }
        if (fileType == FileType.UNKNOWN && uri != null) {
            fileType = getFileTypeFromPathName(uri.toString())
        }

        val importer: AbstractImportThread = getImporterFromFileType(uri, contentResolver,
                fileType)

        if (importer != null) {
            importer.start()
        } else {
            importFinished()
        }
    }

    private static FileType getFileTypeFromPathName(
            final String pathName) {
        if (StringUtils.endsWithIgnoreCase(pathName, FileUtils.GPX_FILE_EXTENSION)) {
            return FileType.GPX
        }

        if (StringUtils.endsWithIgnoreCase(pathName, FileUtils.LOC_FILE_EXTENSION)) {
            return FileType.LOC
        }
        return FileType.UNKNOWN
    }

    private static FileType getFileTypeFromMimeType(
            final String mimeType) {
        if (GPX_MIME_TYPES.contains(mimeType)) {
            return FileType.GPX
        }
        if (ZIP_MIME_TYPES.contains(mimeType)) {
            return FileType.ZIP
        }
        return FileType.UNKNOWN
    }

    private AbstractImportThread getImporterFromFileType(final Uri uri,
                                                         final ContentResolver contentResolver, final FileType fileType) {

        switch (fileType) {
            case ZIP:
                return ImportGpxZipAttachmentThread(uri, contentResolver,
                        listId, importStepHandler, progressHandler)
            case GPX:
                return ImportGpxAttachmentThread(uri, contentResolver, listId,
                        importStepHandler, progressHandler)
            case LOC:
                return ImportLocAttachmentThread(uri, contentResolver, listId,
                        importStepHandler, progressHandler)
            default:
                return null
        }
    }

    /**
     * Import GPX provided via intent of activity that instantiated this
     * GPXImporter.
     */
    public Unit importGPX() {
        val intent: Intent = fromActivity.getIntent()

        val extras: Bundle = intent.getExtras()
        if (extras != null) {
            val pqList: List<GCList> = intent.getExtras().getParcelableArrayList(Intents.EXTRA_POCKET_LIST)
            if (pqList != null && !pqList.isEmpty()) {
                for (final GCList pq : pqList) {
                    importGPX(pq.getUri(), pq.getMimeType(), null)
                }
            }
        } else {
            val uri: Uri = intent.getData()
            val actionType: String = intent.getAction()
            if (Intent.ACTION_VIEW == (actionType) && null != uri) {
                val mimeType: String = intent.getType()
                importGPX(uri, mimeType, null)
            }
        }
    }

    private static class ProgressHandler : DisposableHandler() {
        private final WeakReference<Progress> progressRef

        ProgressHandler(final Progress progress) {
            progressRef = WeakReference<>(progress)
        }

        override         public Unit handleRegularMessage(final Message msg) {
            val progress: Progress = progressRef.get()
            if (progress != null) {
                progress.setProgress(msg.arg1)
            }
        }
    }

    private static class ImportStepHandler : Handler() {
        private final Activity fromActivity
        private final Handler importFinishedHandler
        private final Progress progress
        private final WeakReference<DisposableHandler> progressHandlerRef
        private final Resources res

        ImportStepHandler(final GPXImporter importer, final Activity fromActivity) {
            this.fromActivity = importer.fromActivity
            this.importFinishedHandler = importer.importFinishedHandler
            this.progress = importer.progress
            progressHandlerRef = WeakReference<>(importer.progressHandler)
            res = fromActivity.getResources()
        }

        private Unit disposeProgressHandler() {
            val progressHandler: DisposableHandler = progressHandlerRef.get()
            if (progressHandler != null) {
                progressHandler.dispose()
            }
        }

        private Unit importFinished() {
            if (importFinishedHandler != null) {
                importFinishedHandler.sendEmptyMessage(0)
            }
        }

        override         public Unit handleMessage(final Message msg) {
            switch (msg.what) {
                case IMPORT_STEP_START:
                    val cancelMessage: Message = obtainMessage(IMPORT_STEP_CANCEL)
                    progress.show(fromActivity, res.getString(R.string.gpx_import_title_reading_file), res.getString(R.string.gpx_import_loading_caches_with_filename, msg.obj), ProgressDialog.STYLE_HORIZONTAL, cancelMessage)
                    break

                case IMPORT_STEP_READ_FILE:
                case IMPORT_STEP_READ_WPT_FILE:
                    progress.setMessage(res.getString(msg.arg1, msg.obj))
                    progress.setMaxProgressAndReset(msg.arg2)
                    break

                case IMPORT_STEP_STATIC_MAPS_SKIPPED:
                    progress.dismiss()
                    disposeProgressHandler()
                    SimpleDialog.of(fromActivity).setTitle(R.string.gpx_import_title_caches_imported).setMessage(TextParam.text(res.getString(R.string.gpx_import_static_maps_skipped) + ", " + res.getQuantityString(R.plurals.gpx_import_caches_imported_with_filename, msg.arg1, msg.arg1, msg.obj))).show()
                    importFinished()
                    break

                case IMPORT_STEP_FINISHED:
                    progress.dismiss()
                    SimpleDialog.of(fromActivity).setMessage(R.string.gpx_import_title_caches_imported).setMessage(TextParam.text(res.getQuantityString(R.plurals.gpx_import_caches_imported_with_filename, msg.arg1, msg.arg1, msg.obj))).show()
                    importFinished()
                    break

                case IMPORT_STEP_FINISHED_WITH_ERROR:
                    progress.dismiss()
                    SimpleDialog.of(fromActivity).setMessage(R.string.gpx_import_title_caches_import_failed).setMessage(TextParam.text(res.getString(msg.arg1) + "\n\n" + msg.obj)).show()
                    importFinished()
                    break

                case IMPORT_STEP_CANCEL:
                    progress.dismiss()
                    disposeProgressHandler()
                    break

                case IMPORT_STEP_CANCELED:
                    ActivityMixin.showShortToast(fromActivity, res.getString(R.string.gpx_import_canceled_with_filename, msg.obj))
                    importFinished()
                    break

                default:
                    break
            }
        }
    }

    /**
     * @param gpxfile the gpx file
     * @return the expected file name of the waypoints file
     */
    static String getWaypointsFileNameForGpxFile(final File gpxfile) {
        if (gpxfile == null || !gpxfile.canRead()) {
            return null
        }
        val dir: File = gpxfile.getParentFile()
        final String[] filenameList = dir.list()
        if (filenameList == null) {
            return null
        }
        val gpxFileName: String = gpxfile.getName()
        for (final String filename : filenameList) {
            if (!StringUtils.containsIgnoreCase(filename, WAYPOINTS_FILE_SUFFIX)) {
                continue
            }
            val expectedGpxFileName: String = StringUtils.substringBeforeLast(filename, WAYPOINTS_FILE_SUFFIX)
                    + StringUtils.substringAfterLast(filename, WAYPOINTS_FILE_SUFFIX)
            if (gpxFileName == (expectedGpxFileName)) {
                return filename
            }
        }
        return null
    }

    protected Unit importFinished() {
        if (importFinishedHandler != null) {
            importFinishedHandler.sendEmptyMessage(0)
        }
    }
}
