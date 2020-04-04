package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapSource;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.FileUtils;
import static cgeo.geocaching.utils.FileUtils.getFilenameFromPath;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Receives a map file via intent, moves it to the currently set map directory and sets it as current map source.
 * If no map directory is set currently, default map directory is used, created if needed, and saved as map directory in preferences.
 * If the map file already exists under that name in the map directory, you have the option to either overwrite it or save it under a randomly generated name.
 */
public class ReceiveMapFileActivity extends AbstractActivity {

    private final AtomicBoolean handledSuccessfully = new AtomicBoolean(true);
    private File file = null;
    private Uri uri = null;
    private String mapDirectory = null;

    private static final String MAP_EXTENSION = ".map";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        uri = getIntent().getData();
        final AbstractActivity that = this;

        PermissionHandler.requestStoragePermission(this, new PermissionGrantedCallback(PermissionRequestContext.ReceiveMapFileActivity) {
            @Override
            protected void execute() {
                determineTargetDirectory();
                if (guessFilename()) {
                    handleMapFile(that);
                }
            }
        });
    }

    private void determineTargetDirectory() {
        mapDirectory = Settings.getMapFileDirectory();
        if (mapDirectory == null) {
            final File file = LocalStorage.getDefaultMapDirectory();
            FileUtils.mkdirs(file);
            mapDirectory = file.getPath();
            Settings.setMapFileDirectory(mapDirectory);
        }
    }

    // try to guess a filename, otherwise chose randomized filename
    private boolean guessFilename() {
        try {
            String filename = uri.getPath();    // uri.getLastPathSegment doesn't help here, if path is encoded
            if (filename != null) {
                filename = getFilenameFromPath(filename);
                final int posExt = filename.lastIndexOf('.');
                if (posExt == -1 || !(MAP_EXTENSION.equals(filename.substring(posExt)))) {
                    filename += MAP_EXTENSION;
                }
                file = (new File(mapDirectory, filename)).getCanonicalFile();
            }
            if (file == null) {
                createRandomlyNamedFile();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void handleMapFile(final Activity activity) {
        if (file.exists()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            final AlertDialog dialog = builder.setTitle(R.string.receivemapfile_intenttitle)
                    .setCancelable(true)
                    .setMessage(R.string.receivemapfile_alreadyexists)
                    .setPositiveButton(R.string.receivemapfile_option_overwrite, (dialog3, button3) -> {
                        receiveMapFileInBackground(activity);
                    })
                    .setNegativeButton(R.string.receivemapfile_option_differentname, (dialog2, button2) -> {
                        createRandomlyNamedFile();
                        receiveMapFileInBackground(activity);
                    })
                    .create();
            dialog.setOwnerActivity(activity);
            dialog.show();
        } else {
            receiveMapFileInBackground(activity);
        }
    }

    private void receiveMapFileInBackground(final Activity activity) {
        AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> handledSuccessfully.set(moveMapFile()), () -> {
                String msg = getString(R.string.receivemapfile_error);
                if (handledSuccessfully.get()) {
                    String fileinfo = getFilenameFromPath(file.getPath());
                    fileinfo = fileinfo.substring(0, fileinfo.length() - MAP_EXTENSION.length());
                    msg = String.format(getString(R.string.receivemapfile_success), fileinfo);
                }
                Dialogs.message(activity, getString(R.string.receivemapfile_intenttitle), msg, getString(android.R.string.ok), (dialog, button) -> finish());
            }
        );
    }

    private boolean moveMapFile() {
        try {
            final InputStream inputStream = getContentResolver().openInputStream(uri);
            try {
                // copy file
                file.setWritable(true, false);
                final OutputStream outputStream = new FileOutputStream(file);
                final byte buffer[] = new byte[4096];
                int length = 0;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();

                // remember map file and set it as current map source
                final String newMapPath = file.getPath();
                Settings.setMapFile(newMapPath);
                MapSource newMapSource = null;
                for (final MapSource mapSource : MapProviderFactory.getMapSources()) {
                    if (mapSource instanceof MapsforgeMapProvider.OfflineMapSource && ((MapsforgeMapProvider.OfflineMapSource) mapSource).getFileName().equals(newMapPath)) {
                        newMapSource = mapSource;
                        break;
                    }
                }
                if (newMapSource != null) {
                    Settings.setMapSource(newMapSource);
                }

                return true;
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e) {
        }
        return false;
    }

    private void createRandomlyNamedFile() {
        try {
            file = File.createTempFile("map-", MAP_EXTENSION, new File(mapDirectory));
        } catch (IOException e) {
            file = null;
        }
    }
}
