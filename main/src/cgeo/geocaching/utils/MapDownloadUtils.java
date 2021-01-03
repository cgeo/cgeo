package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.MapDownloadSelectorActivity;
import cgeo.geocaching.storage.ConfigurableFolder;
import cgeo.geocaching.storage.FolderStorage;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.DOWNLOAD_SERVICE;

public class MapDownloadUtils {

    public static final int REQUEST_CODE = 47131;
    public static final String RESULT_CHOSEN_URL = "chosenUrl";
    public static final String RESULT_SIZE_INFO = "sizeInfo";

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
            final String sizeInfo = data.getStringExtra(RESULT_SIZE_INFO);

            if (null != uri) {
                String temp = uri.getLastPathSegment();
                if (null == temp) {
                    temp = "default.map";
                }
                final String filename = temp;

                final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
                builder.setTitle(R.string.downloadmap_title);
                final View layout = View.inflate(activity, R.layout.mapdownloader_confirmation, null);
                builder.setView(layout);
                final TextView downloadInfo = layout.findViewById(R.id.download_info);
                downloadInfo.setText(String.format(activity.getString(R.string.downloadmap_confirmation), filename, sizeInfo));

                builder
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        final boolean allowMeteredNetwork = ((CheckBox) layout.findViewById(R.id.allow_metered_network)).isChecked();

                        final DownloadManager.Request request = new DownloadManager.Request(uri)
                            .setTitle(filename)
                            .setDescription(String.format(activity.getString(R.string.downloadmap_filename), filename))
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                            .setAllowedOverMetered(allowMeteredNetwork)
                            .setAllowedOverRoaming(allowMeteredNetwork);
                        Log.i("Map download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
                        final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                        if (null != downloadManager) {
                            PendingDownload.add(downloadManager.enqueue(request), filename);
                            ActivityMixin.showShortToast(activity, R.string.download_started);
                        } else {
                            ActivityMixin.showToast(activity, R.string.downloadmanager_not_available);
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
            }
            return true;
        }
        return false;
    }

    public interface DirectoryWritable {
        void run (ConfigurableFolder folder, boolean isAvailable);
    }

    public static void checkMapDirectory(final Activity activity, final boolean beforeDownload, final DirectoryWritable callback) {
        PermissionHandler.requestStoragePermission(activity, new PermissionGrantedCallback(PermissionRequestContext.ReceiveMapFileActivity) {
            @Override
            protected void execute() {
                final ConfigurableFolder folder = ConfigurableFolder.OFFLINE_MAPS;
                final boolean mapDirIsReady = FolderStorage.get().checkAndAdjustAvailability(folder);

                if (mapDirIsReady) {
                    callback.run(folder, true);
                } else if (beforeDownload) {
                    Dialogs.confirm(activity, activity.getString(R.string.downloadmap_title), String.format(activity.getString(R.string.downloadmap_target_not_writable), folder), activity.getString(R.string.button_continue),
                            (dialog, which) -> callback.run(folder, true), dialog -> callback.run(folder, false));
                } else {
                    Dialogs.message(activity, activity.getString(R.string.downloadmap_title), String.format(activity.getString(R.string.downloadmap_target_not_writable), folder), activity.getString(android.R.string.ok), (dialog, which) -> callback.run(folder, false));
                }
            }
        });
    }

}
