package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.permission.PermissionGrantedCallback;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Action1;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import static android.content.Context.DOWNLOAD_SERVICE;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class DownloaderUtils {

    public static final String RESULT_CHOSEN_URL = "chosenUrl";
    public static final String RESULT_DATE = "dateInfo";
    public static final String RESULT_TYPEID = "typeId";

    private DownloaderUtils() {
        // utility class
    }

    public static boolean onOptionsItemSelected(final Activity activity, final int id) {
        if (id == R.id.menu_download_offlinemap) {
            activity.startActivity(new Intent(activity, DownloadSelectorActivity.class));
            return true;
        }
        return false;
    }

    public static void checkForRoutingTileUpdates(final Activity activity) {
        if (Settings.useInternalRouting() && Settings.isBrouterAutoTileDownloads() && !PersistableFolder.ROUTING_TILES.isLegacy() && Settings.brouterAutoTileDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, R.string.tileupdate_info, DownloaderUtils::returnFromTileUpdateCheck);
        }
    }

    public static void returnFromTileUpdateCheck(final boolean updateCheckAllowed) {
        Settings.setBrouterAutoTileDownloadsLastCheck(!updateCheckAllowed);
    }

    public static void checkForMapUpdates(final Activity activity) {
        if (Settings.isMapAutoDownloads() && Settings.mapAutoDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, R.string.mapupdate_info, DownloaderUtils::returnFromMapUpdateCheck);
        }
    }

    public static void returnFromMapUpdateCheck(final boolean updateCheckAllowed) {
        Settings.setMapAutoDownloadsLastCheck(!updateCheckAllowed);
    }

    private static String getFilenameFromUri(final Uri uri) {
        String temp = uri.getLastPathSegment();
        if (temp == null) {
            temp = "default.map";
        }
        return temp;
    }

    public static void triggerDownload(final Activity activity, @StringRes final int title, final int type, final Uri uri, final String additionalInfo, final String sizeInfo, @Nullable final Runnable dialogDismissedCallback, @Nullable final Consumer<Long> downloadStartedCallback) {
        final String filename = getFilenameFromUri(uri);
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(title);
        final View layout = View.inflate(activity, R.layout.downloader_confirmation, null);
        builder.setView(layout);
        final TextView downloadInfo = layout.findViewById(R.id.download_info);
        downloadInfo.setText(String.format(activity.getString(R.string.download_confirmation), StringUtils.isNotBlank(additionalInfo) ? additionalInfo + "\n\n" : "", filename, "\n\n" + activity.getString(R.string.download_warning) + (StringUtils.isNotBlank(sizeInfo) ? "\n\n" + sizeInfo : "")));

        builder
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                final boolean allowMeteredNetwork = ((CheckBox) layout.findViewById(R.id.allow_metered_network)).isChecked();
                final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                if (null != downloadManager) {
                    final long id = addDownload(activity, downloadManager, type, uri, filename, allowMeteredNetwork);
                    if (downloadStartedCallback != null) {
                        downloadStartedCallback.accept(id);
                    }

                    // check for required extra files (e. g.: map theme)
                    final AbstractDownloader downloader = Download.DownloadType.getInstance(type);
                    if (downloader != null) {
                        final DownloadDescriptor extraFile = downloader.getExtrafile(activity);
                        if (extraFile != null) {
                            addDownload(activity, downloadManager, extraFile.type, extraFile.uri, extraFile.filename, allowMeteredNetwork);
                        }
                    }

                    ActivityMixin.showShortToast(activity, R.string.download_started);
                } else {
                    ActivityMixin.showToast(activity, R.string.downloadmanager_not_available);
                }
                dialog.dismiss();
                if (dialogDismissedCallback != null) {
                    dialogDismissedCallback.run();
                }
            })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                dialog.dismiss();
                if (dialogDismissedCallback != null) {
                    dialogDismissedCallback.run();
                }
            })
            .create()
            .show();
    }

    private static long addDownload(final Activity activity, final DownloadManager downloadManager, final int type, final Uri uri, final String filename, final boolean allowMeteredNetwork) {
        final DownloadManager.Request request = new DownloadManager.Request(uri)
            .setTitle(filename)
            .setDescription(String.format(activity.getString(R.string.downloadmap_filename), filename))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(allowMeteredNetwork)
            .setAllowedOverRoaming(allowMeteredNetwork);
        Log.i("Download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
        final long id = downloadManager.enqueue(request);
        PendingDownload.add(id, filename, uri.toString(), System.currentTimeMillis(), type);
        return id;
    }

    public static class DownloadDescriptor {
        public final String filename;
        public final Uri uri;
        public final int type;

        DownloadDescriptor(final String filename, final Uri uri, final int type) {
            this.filename = filename;
            this.uri = uri;
            this.type = type;
        }
    }

    public interface DirectoryWritable {
        void run(PersistableFolder folder, boolean isAvailable);
    }

    public static void checkTargetDirectory(final Activity activity, final PersistableFolder folder, final boolean beforeDownload, final DirectoryWritable callback) {
        PermissionHandler.requestStoragePermission(activity, new PermissionGrantedCallback(PermissionRequestContext.ReceiveMapFileActivity) {
            @Override
            protected void execute() {
                final boolean mapDirIsReady = ContentStorage.get().ensureFolder(folder);

                if (mapDirIsReady) {
                    callback.run(folder, true);
                } else if (beforeDownload) {
                    SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).setPositiveButton(TextParam.id(R.string.button_continue)).confirm(
                        (dialog, which) -> callback.run(folder, true), (dialog, w) -> callback.run(folder, false));
                } else {
                    SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).show((dialog, which) -> callback.run(folder, false));
                }
            }
        });
    }

    /**
     * ask user whether to check for updates
     * if yes: checks whether updates are available for the type specified
     *      if yes: ask user to download them all
     *              if yes: trigger download(s)
     * calls callback with user reaction (true=checked for updates / false=user delayed check)
     */
    public static void checkForUpdatesAndDownloadAll(final Activity activity, final Download.DownloadType type, @StringRes final int title, @StringRes final int info, final Action1<Boolean> callback) {
        SimpleDialog.of(activity).setTitle(title).setMessage(info).setNegativeButton(TextParam.id(R.string.later)).confirm((dialog, which) -> {
            new CheckForDownloadsTask(activity, title, type).execute();
            callback.call(true);
        }, (dialog, w) -> callback.call(false));
    }

    // same as checkForUpdatesAndDownloadAll above, but without question
    public static void checkForUpdatesAndDownloadAll(final Activity activity, final Download.DownloadType type, @StringRes final int title, final Action1<Boolean> callback) {
        new CheckForDownloadsTask(activity, title, type).execute();
        callback.call(true);
    }

    private static class CheckForDownloadsTask extends AsyncTaskWithProgressText<Void, List<Download>> {
        private final Download.DownloadType currentType;
        private String lastUrl = "";
        private String lastPage = "";
        private final WeakReference<Activity> activityRef;

        CheckForDownloadsTask(final Activity activity, @StringRes final int title, final Download.DownloadType type) {
            super(activity, activity.getString(title), activity.getString(R.string.downloadmap_checking_for_updates));
            this.activityRef = new WeakReference<>(activity);
            this.currentType = type;
        }

        @Override
        protected List<Download> doInBackgroundInternal(final Void[] none) {
            final List<Download> result = new ArrayList<>();
            final ArrayList<CompanionFileUtils.DownloadedFileData> existingFiles = new ArrayList<>();
            if (currentType.equals(Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED)) {
                final ArrayList<Download.DownloadTypeDescriptor> typeDescriptors =  Download.DownloadType.getOfflineMapTypes();
                for (Download.DownloadTypeDescriptor typeDescriptor : typeDescriptors) {
                    existingFiles.addAll(CompanionFileUtils.availableOfflineMaps(typeDescriptor.type));
                }
            } else {
                existingFiles.addAll(CompanionFileUtils.availableOfflineMaps(currentType));
            }

            for (CompanionFileUtils.DownloadedFileData existingFile : existingFiles) {
                final Download download = checkForUpdate(existingFile);
                if (download != null && download.getDateInfo() > existingFile.remoteDate) {
                    download.setAddInfo(CalendarUtils.yearMonthDay(existingFile.remoteDate));
                    result.add(download);
                }
            }
            return result;
        }

        @Nullable
        private Download checkForUpdate(final CompanionFileUtils.DownloadedFileData offlineMapData) {
            final AbstractDownloader downloader = Download.DownloadType.getInstance(offlineMapData.remoteParsetype);
            if (downloader == null) {
                Log.e("Map update checker: Cannot find map downloader of type " + offlineMapData.remoteParsetype + " for file " + offlineMapData.localFile);
                return null;
            }

            final String url = downloader.getUpdatePageUrl(offlineMapData.remotePage);
            final String page;

            if (url.equals(lastUrl)) {
                page = lastPage;
            } else {
                final Parameters params = new Parameters();
                try {
                    final Response response = Network.getRequest(url, params).blockingGet();
                    page = Network.getResponseData(response, true);

                    if (StringUtils.isBlank(page)) {
                        Log.e("getMap: No data from server");
                        return null;
                    }

                    lastUrl = url;
                    lastPage = page;
                } catch (final Exception e) {
                    return null;
                }
            }

            try {
                return downloader.checkUpdateFor(page, offlineMapData.remotePage, offlineMapData.remoteFile);
            } catch (final Exception e) {
                Log.e("Map update checker: error parsing parsing html page", e);
                return null;
            }
        }

        @Override
        protected void onPostExecuteInternal(final List<Download> result) {
            final Activity activity = activityRef.get();
            if (activity != null) {
                if (result.size() == 0) {
                    Toast.makeText(activity, R.string.no_updates_found, Toast.LENGTH_SHORT).show();
                } else {
                    String updates = "";
                    for (Download download : result) {
                        updates += (StringUtils.isNotBlank(updates) ? ", " : "") + download.getName() + (StringUtils.isNotBlank(download.getSizeInfo()) ? " (" + download.getSizeInfo() + ")" : "");
                    }

                    final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
                    builder.setTitle(R.string.updates_check);
                    final View layout = View.inflate(builder.getContext(), R.layout.downloader_confirmation, null);
                    builder.setView(layout);
                    final TextView downloadInfo = layout.findViewById(R.id.download_info);
                    downloadInfo.setText(String.format(activity.getString(R.string.download_confirmation_updates), updates, "\n\n" + activity.getString(R.string.download_warning)));

                    builder
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            final boolean allowMeteredNetwork = ((CheckBox) layout.findViewById(R.id.allow_metered_network)).isChecked();

                            final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                            if (null != downloadManager) {
                                for (Download download : result) {
                                    final DownloadManager.Request request = new DownloadManager.Request(download.getUri())
                                        .setTitle(download.getName())
                                        .setDescription(String.format(activity.getString(R.string.downloadmap_filename), download.getName()))
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.getName())
                                        .setAllowedOverMetered(allowMeteredNetwork)
                                        .setAllowedOverRoaming(allowMeteredNetwork);
                                    Log.i("Download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + download.getName());
                                    AndroidRxUtils.networkScheduler.scheduleDirect(() -> PendingDownload.add(downloadManager.enqueue(request), download.getName(), download.getUri().toString(), download.getDateInfo(), download.getType().id));
                                }
                                ActivityMixin.showShortToast(activity, R.string.download_started);
                            } else {
                                ActivityMixin.showToast(activity, R.string.downloadmanager_not_available);
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .create()
                        .show();
                }
            }
        }
    }

}
