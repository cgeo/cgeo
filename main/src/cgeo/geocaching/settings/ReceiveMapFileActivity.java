package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.storage.PublicLocalFolder;
import cgeo.geocaching.storage.PublicLocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;
import static cgeo.geocaching.utils.FileUtils.getFilenameFromPath;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
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
                    handleMapFile(that);
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
            filename = PublicLocalFolder.OFFLINE_MAPS.createNewFilename();
        }
        fileinfo = filename;
        if (fileinfo != null) {
            fileinfo = fileinfo.substring(0, fileinfo.length() - MAP_EXTENSION.length());
        }
        return true;
    }

    private void handleMapFile(final Activity activity) {
        //duplicate filenames are handled by PublicLocalStorager automatically
//        if (file.exists()) {
//            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//            final AlertDialog dialog = builder.setTitle(R.string.receivemapfile_intenttitle)
//                    .setCancelable(true)
//                    .setMessage(R.string.receivemapfile_alreadyexists)
//                    .setPositiveButton(R.string.receivemapfile_option_overwrite, (dialog3, button3) -> new CopyTask(this).execute())
//                    .setNeutralButton(R.string.receivemapfile_option_differentname, (dialog2, button2) -> {
//                        createRandomlyNamedFile();
//                        new CopyTask(this).execute();
//                    })
//                    .setNegativeButton(android.R.string.cancel, (dialog4, which4) -> activity.finish())
//                    .create();
//            dialog.setOwnerActivity(activity);
//            dialog.show();
//        } else {
            new CopyTask(this).execute();
 //       }
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
                final Uri outputUri = PublicLocalStorage.get().create(PublicLocalFolder.OFFLINE_MAPS, filename);
                outputStream = PublicLocalStorage.get().openForWrite(outputUri);
                final byte[] buffer = new byte[32 << 10];
                int length = 0;
                while (!cancelled.get() && (length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                    bytesCopied += length;
                    publishProgress(String.format(progressFormat, bytesCopied >> 10));
                }

                // remember map file and set it as current map source
                if (!cancelled.get()) {
                    final String newMapPath = filename; //TODO
                    Settings.setMapFile(newMapPath);
                    MapSource newMapSource = null;
                    for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
                        if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource && ((MapsforgeMapProvider.OfflineMapSource) mapSource).getMapUri()
                            .equals(Uri.fromFile(new File(newMapPath)))) {
                            newMapSource = mapSource;
                            break;
                        }
                    }
                    if (newMapSource != null) {
                        Settings.setMapSource(newMapSource);
                    }
                    status = CopyStates.SUCCESS;
                    getContentResolver().delete(uri, null, null);
                } else {
                    PublicLocalStorage.get().delete(outputUri);
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
                    result = String.format(getString(R.string.receivemapfile_error_io_exception), PublicLocalFolder.OFFLINE_MAPS);
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
