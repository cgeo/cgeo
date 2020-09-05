package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.MapDownloadSelectorActivity;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.DOWNLOAD_SERVICE;

import java.io.File;

public class MapDownloadUtils {

    public static final int REQUEST_CODE = 47131;
    public static final String RESULT_CHOSEN_URL = "chosenUrl";

    private MapDownloadUtils() {
        // utility class
    }

    public static boolean onOptionsItemSelected(final Activity activity, final int id) {
        if (id == R.id.menu_download_offlinemap) {
            activity.startActivityForResult(new Intent(activity, MapDownloadSelectorActivity.class), REQUEST_CODE);
            return true;
        }
        return false;
    }

    public static boolean onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // trigger download manager for downloading the requested file
            final Uri uri = data.getParcelableExtra(RESULT_CHOSEN_URL);
            if (null != uri) {
                String filename = uri.getLastPathSegment();
                if (null == filename) {
                    filename = "default.map";
                }
                final DownloadManager.Request request = new DownloadManager.Request(uri)
                        .setTitle(filename)
                        .setDescription(String.format(activity.getString(R.string.downloadmap_filename), filename))
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true);
                Log.i("Map download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
                final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                if (null != downloadManager) {
                    PendingDownload.add(downloadManager.enqueue(request), filename);
                    ActivityMixin.showShortToast(activity, R.string.download_started);
                } else {
                    ActivityMixin.showToast(activity, R.string.downloadmanager_not_available);
                }
            }
            return true;
        }
        return false;
    }

    public interface DirectoryWritable {
        void run (String path, boolean isWritable);
    }

    public static void checkMapDirectory(final Activity activity, final boolean beforeDownload, final DirectoryWritable callback) {
        PermissionHandler.requestStoragePermission(activity, new PermissionGrantedCallback(PermissionRequestContext.ReceiveMapFileActivity) {
            @Override
            protected void execute() {
                String mapDirectory = Settings.getMapFileDirectory();
                if (mapDirectory == null) {
                    final File file = LocalStorage.getDefaultMapDirectory();
                    FileUtils.mkdirs(file);
                    mapDirectory = file.getPath();
                    Settings.setMapFileDirectory(mapDirectory);
                }
                final String mapFileDirectory = Settings.getMapFileDirectory();
                final boolean canWrite = new File(mapFileDirectory).canWrite();
                if (canWrite) {
                    callback.run(mapFileDirectory, true);
                } else if (beforeDownload) {
                    Dialogs.confirm(activity, activity.getString(R.string.downloadmap_title), String.format(activity.getString(R.string.downloadmap_target_not_writable), mapFileDirectory), "Continue",
                            (dialog, which) -> callback.run(mapFileDirectory, true), dialog -> callback.run(mapFileDirectory, false));
                } else {
                    Dialogs.message(activity, activity.getString(R.string.downloadmap_title), String.format(activity.getString(R.string.downloadmap_target_not_writable), mapFileDirectory), activity.getString(android.R.string.ok), (dialog, which) -> callback.run(mapFileDirectory, false));
                }
            }
        });
    }

}
