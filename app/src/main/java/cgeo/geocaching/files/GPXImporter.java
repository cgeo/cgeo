package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GPXImporter {
    static final int IMPORT_STEP_START = 0;
    static final int IMPORT_STEP_READ_FILE = 1;
    static final int IMPORT_STEP_READ_WPT_FILE = 2;
    static final int IMPORT_STEP_FINISHED = 5;
    static final int IMPORT_STEP_FINISHED_WITH_ERROR = 6;
    static final int IMPORT_STEP_CANCEL = 7;
    static final int IMPORT_STEP_CANCELED = 8;
    static final int IMPORT_STEP_STATIC_MAPS_SKIPPED = 9;

    public static final String WAYPOINTS_FILE_SUFFIX = "-wpts";
    public static final String WAYPOINTS_FILE_SUFFIX_AND_EXTENSION = WAYPOINTS_FILE_SUFFIX + FileUtils.GPX_FILE_EXTENSION;

    private static final List<String> GPX_MIME_TYPES = Arrays.asList("text/xml", "application/xml");
    private static final List<String> ZIP_MIME_TYPES = Arrays.asList("application/zip", "application/x-compressed", "application/x-zip-compressed", "application/x-zip", "application/octet-stream");

    private final Progress progress = new Progress(true);

    private final int listId;
    private final Activity fromActivity;
    private final Handler importFinishedHandler;
    private final DisposableHandler progressHandler = new ProgressHandler(progress);
    private final Handler importStepHandler;

    public GPXImporter(final Activity fromActivity, final int listId, final Handler importFinishedHandler) {
        this.listId = listId;
        this.fromActivity = fromActivity;
        this.importFinishedHandler = importFinishedHandler;
        importStepHandler = new ImportStepHandler(this, fromActivity);
    }

    /**
     * Import GPX file. Currently supports *.gpx, *.zip (containing gpx files, e.g. PQ queries), *.ggz or *.loc files.
     *
     * @param file the file to import
     */
    public void importGPX(final File file) {
        if (StringUtils.endsWithIgnoreCase(file.getName(), FileUtils.GPX_FILE_EXTENSION)) {
            new ImportGpxFileThread(file, listId, importStepHandler, progressHandler).start();
        } else if (StringUtils.endsWithIgnoreCase(file.getName(), FileUtils.ZIP_FILE_EXTENSION) || StringUtils.endsWithIgnoreCase(file.getName(), FileUtils.COMPRESSED_GPX_FILE_EXTENSION)) {
            new ImportGpxZipFileThread(file, listId, importStepHandler, progressHandler).start();
        } else {
            new ImportLocFileThread(file, listId, importStepHandler, progressHandler).start();
        }
    }

    /**
     * Import GPX file from URI.
     *
     * @param uri URI of the file to import
     */
    public void importGPX(final Uri uri, @Nullable final String mimeType, @Nullable final String pathName) {
        final ContentResolver contentResolver = fromActivity.getContentResolver();

        Log.i("importGPX: " + uri + ", mimetype=" + mimeType);

        FileType fileType = new FileTypeDetector(uri, contentResolver)
                .getFileType();

        if (fileType == FileType.UNKNOWN) {
            fileType = getFileTypeFromPathName(pathName);
        }
        if (fileType == FileType.UNKNOWN) {
            fileType = getFileTypeFromMimeType(mimeType);
        }
        if (fileType == FileType.UNKNOWN && uri != null) {
            fileType = getFileTypeFromPathName(uri.toString());
        }

        final AbstractImportThread importer = getImporterFromFileType(uri, contentResolver,
                fileType);

        if (importer != null) {
            importer.start();
        } else {
            importFinished();
        }
    }

    @NonNull
    private static FileType getFileTypeFromPathName(
            final String pathName) {
        if (StringUtils.endsWithIgnoreCase(pathName, FileUtils.GPX_FILE_EXTENSION)) {
            return FileType.GPX;
        }

        if (StringUtils.endsWithIgnoreCase(pathName, FileUtils.LOC_FILE_EXTENSION)) {
            return FileType.LOC;
        }
        return FileType.UNKNOWN;
    }

    @NonNull
    private static FileType getFileTypeFromMimeType(
            final String mimeType) {
        if (GPX_MIME_TYPES.contains(mimeType)) {
            return FileType.GPX;
        }
        if (ZIP_MIME_TYPES.contains(mimeType)) {
            return FileType.ZIP;
        }
        return FileType.UNKNOWN;
    }

    private AbstractImportThread getImporterFromFileType(final Uri uri,
                                                         final ContentResolver contentResolver, final FileType fileType) {

        switch (fileType) {
            case ZIP:
                return new ImportGpxZipAttachmentThread(uri, contentResolver,
                        listId, importStepHandler, progressHandler);
            case GPX:
                return new ImportGpxAttachmentThread(uri, contentResolver, listId,
                        importStepHandler, progressHandler);
            case LOC:
                return new ImportLocAttachmentThread(uri, contentResolver, listId,
                        importStepHandler, progressHandler);
            default:
                return null;
        }
    }

    /**
     * Import GPX provided via intent of activity that instantiated this
     * GPXImporter.
     */
    public void importGPX() {
        final Intent intent = fromActivity.getIntent();
        final Uri uri = intent.getData();
        final String mimeType = intent.getType();
        importGPX(uri, mimeType, null);
    }

    private static final class ProgressHandler extends DisposableHandler {
        private final WeakReference<Progress> progressRef;

        ProgressHandler(final Progress progress) {
            progressRef = new WeakReference<>(progress);
        }

        @Override
        public void handleRegularMessage(final Message msg) {
            final Progress progress = progressRef.get();
            if (progress != null) {
                progress.setProgress(msg.arg1);
            }
        }
    }

    private static final class ImportStepHandler extends Handler {
        private final Activity fromActivity;
        private final Handler importFinishedHandler;
        private final Progress progress;
        private final WeakReference<DisposableHandler> progressHandlerRef;
        private final Resources res;

        ImportStepHandler(final GPXImporter importer, final Activity fromActivity) {
            this.fromActivity = importer.fromActivity;
            this.importFinishedHandler = importer.importFinishedHandler;
            this.progress = importer.progress;
            progressHandlerRef = new WeakReference<>(importer.progressHandler);
            res = fromActivity.getResources();
        }

        private void disposeProgressHandler() {
            final DisposableHandler progressHandler = progressHandlerRef.get();
            if (progressHandler != null) {
                progressHandler.dispose();
            }
        }

        private void importFinished() {
            if (importFinishedHandler != null) {
                importFinishedHandler.sendEmptyMessage(0);
            }
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case IMPORT_STEP_START:
                    final Message cancelMessage = obtainMessage(IMPORT_STEP_CANCEL);
                    progress.show(fromActivity, res.getString(R.string.gpx_import_title_reading_file), res.getString(R.string.gpx_import_loading_caches_with_filename, msg.obj), ProgressDialog.STYLE_HORIZONTAL, cancelMessage);
                    break;

                case IMPORT_STEP_READ_FILE:
                case IMPORT_STEP_READ_WPT_FILE:
                    progress.setMessage(res.getString(msg.arg1, msg.obj));
                    progress.setMaxProgressAndReset(msg.arg2);
                    break;

                case IMPORT_STEP_STATIC_MAPS_SKIPPED:
                    progress.dismiss();
                    disposeProgressHandler();
                    SimpleDialog.of(fromActivity).setTitle(R.string.gpx_import_title_caches_imported).setMessage(TextParam.text(res.getString(R.string.gpx_import_static_maps_skipped) + ", " + res.getQuantityString(R.plurals.gpx_import_caches_imported_with_filename, msg.arg1, msg.arg1, msg.obj))).show();
                    importFinished();
                    break;

                case IMPORT_STEP_FINISHED:
                    progress.dismiss();
                    SimpleDialog.of(fromActivity).setMessage(R.string.gpx_import_title_caches_imported).setMessage(TextParam.text(res.getQuantityString(R.plurals.gpx_import_caches_imported_with_filename, msg.arg1, msg.arg1, msg.obj))).show();
                    importFinished();
                    break;

                case IMPORT_STEP_FINISHED_WITH_ERROR:
                    progress.dismiss();
                    SimpleDialog.of(fromActivity).setMessage(R.string.gpx_import_title_caches_import_failed).setMessage(TextParam.text(res.getString(msg.arg1) + "\n\n" + msg.obj)).show();
                    importFinished();
                    break;

                case IMPORT_STEP_CANCEL:
                    progress.dismiss();
                    disposeProgressHandler();
                    break;

                case IMPORT_STEP_CANCELED:
                    ActivityMixin.showShortToast(fromActivity, res.getString(R.string.gpx_import_canceled_with_filename, msg.obj));
                    importFinished();
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * @param gpxfile the gpx file
     * @return the expected file name of the waypoints file
     */
    static String getWaypointsFileNameForGpxFile(final File gpxfile) {
        if (gpxfile == null || !gpxfile.canRead()) {
            return null;
        }
        final File dir = gpxfile.getParentFile();
        final String[] filenameList = dir.list();
        if (filenameList == null) {
            return null;
        }
        final String gpxFileName = gpxfile.getName();
        for (final String filename : filenameList) {
            if (!StringUtils.containsIgnoreCase(filename, WAYPOINTS_FILE_SUFFIX)) {
                continue;
            }
            final String expectedGpxFileName = StringUtils.substringBeforeLast(filename, WAYPOINTS_FILE_SUFFIX)
                    + StringUtils.substringAfterLast(filename, WAYPOINTS_FILE_SUFFIX);
            if (gpxFileName.equals(expectedGpxFileName)) {
                return filename;
            }
        }
        return null;
    }

    protected void importFinished() {
        if (importFinishedHandler != null) {
            importFinishedHandler.sendEmptyMessage(0);
        }
    }
}
