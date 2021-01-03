package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.storage.ConfigurableFolder;
import cgeo.geocaching.storage.FolderStorage;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.FileNameCreator;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;
import static cgeo.geocaching.utils.FileUtils.getFilenameFromPath;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Receives a map file via intent, moves it to the currently set map directory and sets it as current map source.
 * If no map directory is set currently, default map directory is used, created if needed, and saved as map directory in preferences.
 * If the map file already exists under that name in the map directory, you have the option to either overwrite it or save it under a randomly generated name.
 */
public class ReceiveMapFileActivity extends AbstractActivity {

    public static final String EXTRA_FILENAME = "filename";

    private Uri uri = null;
    private String filename = null;
    private String fileinfo = "";

    private static final String MAP_EXTENSION = ".map";

    protected enum CopyStates {
        SUCCESS, CANCELLED, IO_EXCEPTION, FILENOTFOUND_EXCEPTION, UNKNOWN_STATE
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Intent intent = getIntent();
        uri = intent.getData();
        final String preset = intent.getStringExtra(EXTRA_FILENAME);
        final AbstractActivity that = this;

        MapDownloadUtils.checkMapDirectory(this, false, (folder, isWritable) -> {
            if (isWritable) {
                if (guessFilename(preset)) {
                    handleMapFile();
                }
            } else {
                finish();
            }
        });
    }

    // try to guess a filename, otherwise chose randomized filename
    private boolean guessFilename(final String preset) {
        filename = StringUtils.isNotBlank(preset) ? preset : uri.getPath();    // uri.getLastPathSegment doesn't help here, if path is encoded
        if (filename != null) {
            filename = getFilenameFromPath(filename);
            final int posExt = filename.lastIndexOf('.');
            if (posExt == -1 || !(MAP_EXTENSION.equals(filename.substring(posExt)))) {
                filename += MAP_EXTENSION;
            }
        }
        if (filename == null) {
            filename = FileNameCreator.OFFLINE_MAPS.createName();
        }
        fileinfo = filename;
        if (fileinfo != null) {
            fileinfo = fileinfo.substring(0, fileinfo.length() - MAP_EXTENSION.length());
        }
        return true;
    }

    private void handleMapFile() {
        //duplicate filenames are handled by FolderStorage automatically
        new CopyTask(this).execute();
    }

    protected class CopyTask extends AsyncTaskWithProgressText<String, CopyStates> {
        private long bytesCopied = 0;
        private final String progressFormat = getString(R.string.receivemapfile_kb_copied);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final Activity context;

        CopyTask(final Activity activity) {
            super(activity, activity.getString(R.string.receivemapfile_intenttitle), "");
            setOnCancelListener((dialog, which) -> cancelled.set(true));
            context = activity;
        }

        @Override
        protected CopyStates doInBackgroundInternal(final String[] logTexts) {
            CopyStates status = CopyStates.UNKNOWN_STATE;

            Log.d("start receiving map file: " + filename);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);
                // copy file
                final Uri outputUri = FolderStorage.get().create(ConfigurableFolder.OFFLINE_MAPS, filename);
                outputStream = FolderStorage.get().openForWrite(outputUri);
                final byte[] buffer = new byte[32 << 10];
                int length = 0;
                while (!cancelled.get() && (length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                    bytesCopied += length;
                    publishProgress(String.format(progressFormat, bytesCopied >> 10));
                }

                // clean up and refresh available map list
                if (!cancelled.get()) {
                    status = CopyStates.SUCCESS;
                    try {
                        getContentResolver().delete(uri, null, null);
                    } catch (IllegalArgumentException iae) {
                        Log.w("Deleting Uri '" + uri + "' failed, will be ignored", iae);
                    }
                    //update offline maps AFTER deleting source file. This handles the very special case when Map Folder = Download Folder
                    MapsforgeMapProvider.getInstance().updateOfflineMaps(outputUri);
                } else {
                    FolderStorage.get().delete(outputUri);
                    status = CopyStates.CANCELLED;
                }
            } catch (FileNotFoundException e) {
                Log.e("FileNotFoundException on receiving map file: " + e.getMessage());
                status = CopyStates.FILENOTFOUND_EXCEPTION;
            } catch (IOException e) {
                Log.e("IOException on receiving map file: " + e.getMessage());
                status = CopyStates.IO_EXCEPTION;
            } finally {
                IOUtils.closeQuietly(inputStream, outputStream);
            }

            return status;
        }

        @Override
        protected void onPostExecuteInternal(final CopyStates status) {
            final String result;
            switch (status) {
                case SUCCESS:
                    result = String.format(getString(R.string.receivemapfile_success), fileinfo);
                    break;
                case CANCELLED:
                    result = getString(R.string.receivemapfile_cancelled);
                    break;
                case IO_EXCEPTION:
                    result = String.format(getString(R.string.receivemapfile_error_io_exception), ConfigurableFolder.OFFLINE_MAPS);
                    break;
                case FILENOTFOUND_EXCEPTION:
                    result = getString(R.string.receivemapfile_error_filenotfound_exception);
                    break;
                default:
                    result = getString(R.string.receivemapfile_error);
                    break;

            }
            Dialogs.message(context, getString(R.string.receivemapfile_intenttitle), result, getString(android.R.string.ok), (dialog, button) -> finish());
        }

    }

}
