package cgeo.geocaching.files;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class GPXImporter {
    static final int IMPORT_STEP_START = 0;
    static final int IMPORT_STEP_READ_FILE = 1;
    static final int IMPORT_STEP_READ_WPT_FILE = 2;
    static final int IMPORT_STEP_STORE_STATIC_MAPS = 4;
    static final int IMPORT_STEP_FINISHED = 5;
    static final int IMPORT_STEP_FINISHED_WITH_ERROR = 6;
    static final int IMPORT_STEP_CANCEL = 7;
    static final int IMPORT_STEP_CANCELED = 8;
    static final int IMPORT_STEP_STATIC_MAPS_SKIPPED = 9;

    public static final String GPX_FILE_EXTENSION = ".gpx";
    public static final String LOC_FILE_EXTENSION = ".loc";
    public static final String ZIP_FILE_EXTENSION = ".zip";
    public static final String WAYPOINTS_FILE_SUFFIX = "-wpts";
    public static final String WAYPOINTS_FILE_SUFFIX_AND_EXTENSION = WAYPOINTS_FILE_SUFFIX + GPX_FILE_EXTENSION;

    private static final List<String> GPX_MIME_TYPES = Arrays.asList("text/xml", "application/xml");
    private static final List<String> ZIP_MIME_TYPES = Arrays.asList("application/zip", "application/x-compressed", "application/x-zip-compressed", "application/x-zip", "application/octet-stream");

    private final Progress progress = new Progress(true);

    private final Resources res;
    private final int listId;
    private final Activity fromActivity;
    private final Handler importFinishedHandler;

    public GPXImporter(final Activity fromActivity, final int listId, final Handler importFinishedHandler) {
        this.listId = listId;
        this.fromActivity = fromActivity;
        res = fromActivity.getResources();
        this.importFinishedHandler = importFinishedHandler;
    }

    /**
     * Import GPX file. Currently supports *.gpx, *.zip (containing gpx files, e.g. PQ queries) or *.loc files.
     *
     * @param file
     *            the file to import
     */
    public void importGPX(final File file) {
        if (StringUtils.endsWithIgnoreCase(file.getName(), GPX_FILE_EXTENSION)) {
            new ImportGpxFileThread(file, listId, importStepHandler, progressHandler).start();
        } else if (StringUtils.endsWithIgnoreCase(file.getName(), ZIP_FILE_EXTENSION)) {
            new ImportGpxZipFileThread(file, listId, importStepHandler, progressHandler).start();
        } else {
            new ImportLocFileThread(file, listId, importStepHandler, progressHandler).start();
        }
    }

    /**
     * Import GPX file from URI.
     *
     * @param uri
     *            URI of the file to import
     */
    public void importGPX(final Uri uri, final @Nullable String mimeType, final @Nullable String pathName) {
        final ContentResolver contentResolver = fromActivity.getContentResolver();

        Log.i("importGPX: " + uri + ", mimetype=" + mimeType);
		@NonNull
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

	private static @NonNull FileType getFileTypeFromPathName(
			final String pathName) {
        if (StringUtils.endsWithIgnoreCase(pathName, GPX_FILE_EXTENSION)) {
        		return FileType.GPX;
        }

	if (StringUtils.endsWithIgnoreCase(pathName, LOC_FILE_EXTENSION)) {
		return FileType.LOC;
	}
		return FileType.UNKNOWN;
	}

	private static @NonNull FileType getFileTypeFromMimeType(
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

    final private CancellableHandler progressHandler = new CancellableHandler() {
        @Override
        public void handleRegularMessage(final Message msg) {
            progress.setProgress(msg.arg1);
        }
    };

    final private Handler importStepHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case IMPORT_STEP_START:
                    final Message cancelMessage = importStepHandler.obtainMessage(IMPORT_STEP_CANCEL);
                    progress.show(fromActivity, res.getString(R.string.gpx_import_title_reading_file), res.getString(R.string.gpx_import_loading_caches), ProgressDialog.STYLE_HORIZONTAL, cancelMessage);
                    break;

                case IMPORT_STEP_READ_FILE:
                case IMPORT_STEP_READ_WPT_FILE:
                    progress.setMessage(res.getString(msg.arg1));
                    progress.setMaxProgressAndReset(msg.arg2);
                    break;

                case IMPORT_STEP_STORE_STATIC_MAPS:
                    progress.dismiss();
                    final Message skipMessage = importStepHandler.obtainMessage(IMPORT_STEP_STATIC_MAPS_SKIPPED, msg.arg2, 0);
                    progress.show(fromActivity, res.getString(R.string.gpx_import_title_static_maps), res.getString(R.string.gpx_import_store_static_maps), ProgressDialog.STYLE_HORIZONTAL, skipMessage);
                    progress.setMaxProgressAndReset(msg.arg2);
                    break;

                case IMPORT_STEP_STATIC_MAPS_SKIPPED:
                    progress.dismiss();
                    progressHandler.cancel();
                    final StringBuilder bufferSkipped = new StringBuilder(20);
                    bufferSkipped.append(res.getString(R.string.gpx_import_static_maps_skipped)).append(", ").append(msg.arg1).append(' ').append(res.getString(R.string.gpx_import_caches_imported));
                    Dialogs.message(fromActivity, R.string.gpx_import_title_caches_imported, bufferSkipped.toString());
                    importFinished();
                    break;

                case IMPORT_STEP_FINISHED:
                    progress.dismiss();
                    Dialogs.message(fromActivity, R.string.gpx_import_title_caches_imported, msg.arg1 + " " + res.getString(R.string.gpx_import_caches_imported));
                    importFinished();
                    break;

                case IMPORT_STEP_FINISHED_WITH_ERROR:
                    progress.dismiss();
                    Dialogs.message(fromActivity, R.string.gpx_import_title_caches_import_failed, res.getString(msg.arg1) + "\n\n" + msg.obj);
                    importFinished();
                    break;

                case IMPORT_STEP_CANCEL:
                    progress.dismiss();
                    progressHandler.cancel();
                    break;

                case IMPORT_STEP_CANCELED:
                    final StringBuilder bufferCanceled = new StringBuilder(20);
                    bufferCanceled.append(res.getString(R.string.gpx_import_canceled));
                    ActivityMixin.showShortToast(fromActivity, bufferCanceled.toString());
                    importFinished();
                    break;

                default:
                    break;
            }
        }
    };

    /**
     * @param gpxfile
     *            the gpx file
     * @return the expected file name of the waypoints file
     */
    static String getWaypointsFileNameForGpxFile(final File gpxfile) {
        if (gpxfile == null || !gpxfile.canRead()) {
            return null;
        }
        final String gpxFileName = gpxfile.getName();
        final File dir = gpxfile.getParentFile();
        final String[] filenameList = dir.list();
        if (filenameList == null) {
            return null;
        }
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
