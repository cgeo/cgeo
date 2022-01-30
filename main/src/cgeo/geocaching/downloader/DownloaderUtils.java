package cgeo.geocaching.downloader;

import cgeo.geocaching.Intents;
import cgeo.geocaching.MainActivity;
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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import static android.content.Context.DOWNLOAD_SERVICE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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

    public static void checkForRoutingTileUpdates(final MainActivity activity) {
        if (Settings.useInternalRouting() && !PersistableFolder.ROUTING_TILES.isLegacy() && Settings.brouterAutoTileDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, R.id.tilesupdate, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, R.string.tileupdate_info, DownloaderUtils::returnFromTileUpdateCheck);
        }
    }

    public static void returnFromTileUpdateCheck(final boolean updateCheckAllowed) {
        Settings.setBrouterAutoTileDownloadsLastCheck(!updateCheckAllowed);
    }

    public static void checkForMapUpdates(final MainActivity activity) {
        if (Settings.mapAutoDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, R.id.mapupdate, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, R.string.mapupdate_info, DownloaderUtils::returnFromMapUpdateCheck);
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

    public static void triggerDownloads(final Activity activity, @StringRes final int title, @StringRes final int confirmation, final List<Download> downloads) {
        String updates = "";
        for (Download download : downloads) {
            updates += (StringUtils.isNotBlank(updates) ? ", " : "") + download.getName() + (StringUtils.isNotBlank(download.getSizeInfo()) ? " (" + download.getSizeInfo() + ")" : "");
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(title);
        final View layout = View.inflate(builder.getContext(), R.layout.downloader_confirmation, null);
        builder.setView(layout);
        final TextView downloadInfo = layout.findViewById(R.id.download_info);
        downloadInfo.setText(String.format(activity.getString(confirmation), updates, "\n\n" + activity.getString(R.string.download_warning)));

        builder
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                final boolean allowMeteredNetwork = ((CheckBox) layout.findViewById(R.id.allow_metered_network)).isChecked();

                final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                if (null != downloadManager) {
                    for (Download download : downloads) {
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
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
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
     * displays an info to the user to check for updates
     * if button pressed: checks whether updates are available for the type specified
     *      if yes: ask user to download them all
     *              if yes: trigger download(s)
     */
    public static void checkForUpdatesAndDownloadAll(final MainActivity activity, final int layout, final Download.DownloadType type, @StringRes final int title, @StringRes final int info, final Action1<Boolean> callback) {
        activity.displayActionItem(layout, info, () -> {
            new CheckForDownloadsTask(activity, title, type).execute();
            callback.call(true);
        });
    }

    // execute download check
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
                final ArrayList<Download.DownloadTypeDescriptor> typeDescriptors =  Download.DownloadType.getOfflineAllMapRelatedTypes();
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
                    triggerDownloads(activity, R.string.updates_check, R.string.download_confirmation_updates, result);
                }
            }
        }
    }

    /**
     * check for successfully completed, but not yet received downloads
     * and offer to download them
     */
    public static void checkPendingDownloads(final Activity activity) {
        final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        if (null != downloadManager) {
            final DownloadManager.Query query = new DownloadManager.Query();
            try (Cursor c = downloadManager.query(query)) {
                final int columnId = c.getColumnIndex(DownloadManager.COLUMN_ID);
                final int columnStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                final int columnBytesA = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                final int columnBytesT = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                while (c.moveToNext()) {
                    final int status = c.getInt(columnStatus);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        final int bytesA = c.getInt(columnBytesA);
                        final int bytesT = c.getInt(columnBytesT);
                        if (bytesA == bytesT) {
                            final long id = c.getLong(columnId);
                            final PendingDownload p = PendingDownload.load(id);
                            if (p != null) {
                                startReceive(activity, downloadManager, id, p);
                                Log.d("download #" + id + " retriggered manually");
                            }
                        }
                    }
                }
            }
        }
    }

    public static void startReceive(final Context context, final DownloadManager downloadManager, final long id, final PendingDownload pendingDownload) {
        PendingDownload.remove(id);
        final Intent copyFileIntent = new Intent(context, ReceiveDownloadService.class);
        final Uri uri = downloadManager.getUriForDownloadedFile(id);
        copyFileIntent.setData(uri);
        copyFileIntent.putExtra(Intents.EXTRA_FILENAME, pendingDownload.getFilename());
        copyFileIntent.putExtra(DownloaderUtils.RESULT_CHOSEN_URL, pendingDownload.getRemoteUrl());
        copyFileIntent.putExtra(DownloaderUtils.RESULT_DATE, pendingDownload.getDate());
        copyFileIntent.putExtra(DownloaderUtils.RESULT_TYPEID, pendingDownload.getOfflineMapTypeId());
        ContextCompat.startForegroundService(context, copyFileIntent);
    }

    public static void dumpDownloadmanagerInfos(@NonNull final Activity activity) {
        final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        final DownloadManager.Query query = new DownloadManager.Query();
        final StringBuilder sb = new StringBuilder();
        try (Cursor c = downloadManager.query(query)) {
            final int columnStatus = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
            final int columnReason = c.getColumnIndex(DownloadManager.COLUMN_REASON);
            final int[] columns = {
                c.getColumnIndex(DownloadManager.COLUMN_ID),
                c.getColumnIndex(DownloadManager.COLUMN_TITLE),
                columnStatus,
                columnReason,
                c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                c.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP),
                c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE),
                c.getColumnIndex(DownloadManager.COLUMN_URI),
                c.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI),
                c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            };
            while (c.moveToNext()) {
                for (int column : columns) {
                    sb.append("- ").append(c.getColumnName(column)).append(" = ");
                    if (column == columnStatus) {
                        sb.append(c.getString(column));
                        final int status = c.getInt(column);
                        if (status == DownloadManager.STATUS_FAILED) {
                            sb.append(" - download has failed (and will not be retried)");
                        } else if (status == DownloadManager.STATUS_PAUSED) {
                            sb.append(" - download is waiting to retry or resume");
                        } else if (status == DownloadManager.STATUS_PENDING) {
                            sb.append(" - download is waiting to start");
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            sb.append(" - download is currently running");
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            sb.append(" - download has successfully completed");
                        }
                    } else if (column == columnReason) {
                        final int reason = c.getInt(column);
                        sb.append(reason);
                        if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                            sb.append(" - paused: download exceeds a size limit for downloads over mobile network / waiting for Wifi");
                        } else if (reason == DownloadManager.PAUSED_UNKNOWN) {
                            sb.append(" - paused: for some other reason");
                        } else if (reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) {
                            sb.append(" - paused: waiting for network connectivity to proceed");
                        } else if (reason == DownloadManager.PAUSED_WAITING_TO_RETRY) {
                            sb.append(" - paused: some network error occurred and the download manager is waiting before retrying the request");
                        } else if (reason == DownloadManager.ERROR_CANNOT_RESUME) {
                            sb.append(" - error: some possibly transient error occurred but we can't resume the download");
                        } else if (reason == DownloadManager.ERROR_DEVICE_NOT_FOUND) {
                            sb.append(" - error: no external storage device was found. SD card mounted?");
                        } else if (reason == DownloadManager.ERROR_FILE_ALREADY_EXISTS) {
                            sb.append(" - error: requested destination file already exists, will not be overwritten");
                        } else if (reason == DownloadManager.ERROR_FILE_ERROR) {
                            sb.append(" - error: unknown storage issue");
                        } else if (reason == DownloadManager.ERROR_HTTP_DATA_ERROR) {
                            sb.append(" - error: HTTP processing error at data level");
                        } else if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                            sb.append(" - error: insufficient storage space");
                        } else if (reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
                            sb.append(" - error: too many redirects");
                        } else if (reason == DownloadManager.ERROR_UNHANDLED_HTTP_CODE) {
                            sb.append(" - error: unhandled HTTP cod");
                        } else if (reason == DownloadManager.ERROR_UNKNOWN) {
                            sb.append(" - error: unknown error");
                        }

                    } else {
                        sb.append(c.getString(column));
                    }
                    sb.append("\n");
                }
                sb.append("\n---\n\n");
            }
        }

        SimpleDialog.of(activity).setTitle(R.string.debug_current_downloads).setMessage(TextParam.text(sb.toString()).setMarkdown(true)).show();
    }

}
