package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Intents;
import cgeo.geocaching.MainActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.DownloaderConfirmationBinding;
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.storage.extension.PendingDownload;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.OfflineTranslateUtils;
import cgeo.geocaching.utils.functions.Action1;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_HILLSHADING_TILES;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_LANGUAGE_MODEL;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOAD_TYPE_ALL_MAPS;
import static cgeo.geocaching.models.Download.DownloadType.DOWNLOAD_TYPE_ALL_THEMES;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import static android.content.Context.DOWNLOAD_SERVICE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
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
        } else if (id == R.id.menu_download_backgroundmap) {
            final Intent intent = new Intent(activity, DownloadSelectorActivity.class);
            intent.putExtra(DownloadSelectorActivity.INTENT_FIXED_DOWNLOADTYPE, Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id);
            activity.startActivity(intent);
            return true;
        } else if (id == R.id.menu_delete_offline_data) {
            deleteOfflineData(activity);
            return true;
        }
        return false;
    }

    public static void checkForRoutingTileUpdates(final MainActivity activity) {
        if (Settings.useInternalRouting() && !PersistableFolder.ROUTING_TILES.isLegacy() && Settings.brouterAutoTileDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, R.id.tilesupdate, DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, R.string.tileupdate_info, DownloaderUtils::returnFromTileUpdateCheck);
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
        final DownloaderConfirmationBinding binding = DownloaderConfirmationBinding.inflate(activity.getLayoutInflater());
        builder.setView(binding.getRoot());
        binding.downloadInfo1.setText(String.format(activity.getString(R.string.download_confirmation), StringUtils.isNotBlank(additionalInfo) ? additionalInfo + "\n\n" : "", filename, "\n\n" + activity.getString(R.string.download_warning) + (StringUtils.isNotBlank(sizeInfo) ? "\n\n" + sizeInfo : "")));
        binding.downloadInfo2.setVisibility(View.GONE);

        builder
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final boolean allowMeteredNetwork = binding.allowMeteredNetwork.isChecked();
                    final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                    if (null != downloadManager) {
                        final long id = addDownload(activity, downloadManager, type, uri, filename, allowMeteredNetwork);
                        if (id != -1) {
                            if (downloadStartedCallback != null) {
                                downloadStartedCallback.accept(id);
                            }

                            // check for required extra files (e. g.: map theme)
                            final AbstractDownloader downloader = Download.DownloadType.getInstance(type);
                            if (downloader != null) {
                                final DownloadDescriptor extraFile = downloader.getExtrafile(activity, uri);
                                if (extraFile != null) {
                                    addDownload(activity, downloadManager, extraFile.type, extraFile.uri, extraFile.filename, allowMeteredNetwork);
                                }
                            }
                            ActivityMixin.showShortToast(activity, R.string.download_started);
                        } else {
                            ActivityMixin.showShortToast(activity, R.string.download_enqueing_error);
                        }
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

    @SuppressLint("SetTextI18n")
    public static void triggerDownloads(final Activity activity, @StringRes final int title, @StringRes final int confirmation, final List<Download> downloads, @Nullable final Action1<Boolean> downloadTriggered) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity);
        builder.setTitle(title);
        final DownloaderConfirmationBinding binding = DownloaderConfirmationBinding.inflate(activity.getLayoutInflater());
        builder.setView(binding.getRoot());
        binding.downloadInfo1.setText(confirmation);
        binding.downloadInfo2.setText(R.string.download_warning);

        for (Download download : downloads) {
            final CheckBox cb = new CheckBox(new ContextThemeWrapper(activity, R.style.checkbox_full));
            final String sizeinfo = download.getSizeInfo();
            cb.setText(download.getName() + (StringUtils.isNotBlank(sizeinfo) ? " (" + sizeinfo + ")" : ""));
            cb.setChecked(true);
            download.customMarker = true;

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cb.setChecked(isChecked);
                download.customMarker = isChecked;
                Log.i(download.getName() + ", checked=" + isChecked);
            });
            binding.checkboxPlaceholder.addView(cb);
        }

        builder
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final boolean allowMeteredNetwork = binding.allowMeteredNetwork.isChecked();

                    final DownloadManager downloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                    if (null != downloadManager) {
                        int numFiles = 0;
                        for (Download download : downloads) {
                            if (download.customMarker) {
                                numFiles++;
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
                        }
                        ActivityMixin.showShortToast(activity, numFiles > 0 ? R.string.download_started : R.string.no_files_selected);
                        if (downloadTriggered != null) {
                            downloadTriggered.call(true);
                        }
                    } else {
                        ActivityMixin.showToast(activity, R.string.downloadmanager_not_available);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (downloadTriggered != null) {
                        downloadTriggered.call(false);
                    }
                    dialog.dismiss();
                })
                .create()
                .show();
    }

    private static long addDownload(final Activity activity, final DownloadManager downloadManager, final int type, final Uri uri, final String filename, final boolean allowMeteredNetwork) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !PermissionContext.LEGACY_WRITE_EXTERNAL_STORAGE.hasAllPermissions()) {
            // those versions still need WRITE_EXTERNAL_STORAGE permission to enqueue a download
            SimpleDialog.ofContext(activity).setTitle(TextParam.id(R.string.permission_missing)).setMessage(TextParam.id(R.string.storage_permission_needed)).show();
            return -1;
        }
        final DownloadManager.Request request = new DownloadManager.Request(uri)
                .setTitle(filename)
                .setDescription(String.format(activity.getString(R.string.downloadmap_filename), filename))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
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
        final boolean mapDirIsReady = ContentStorage.get().ensureFolder(folder);

        if (mapDirIsReady) {
            callback.run(folder, true);
        } else if (beforeDownload) {
            SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).setPositiveButton(TextParam.id(R.string.button_continue)).confirm(
                    () -> callback.run(folder, true), () -> callback.run(folder, false));
        } else {
            SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).show(() -> callback.run(folder, false));
        }
    }

    /**
     * displays an info to the user to check for updates
     * if button pressed: checks whether updates are available for the type specified
     * if yes: ask user to download them all
     * if yes: trigger download(s)
     */
    public static void checkForUpdatesAndDownloadAll(final MainActivity activity, final int layout, final Download.DownloadType type, @StringRes final int title, @StringRes final int info, final Action1<Boolean> callback) {
        activity.displayActionItem(layout, info, true, (actionRequested) -> {
            if (actionRequested) {
                new CheckForDownloadsTask(activity, title, type).execute();
            }
            callback.call(actionRequested);
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
                final ArrayList<Download.DownloadTypeDescriptor> typeDescriptors = Download.DownloadType.getOfflineAllMapRelatedTypes();
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
        @WorkerThread
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
                if (result.isEmpty()) {
                    ViewUtils.showShortToast(activity, R.string.no_updates_found);
                } else {
                    triggerDownloads(activity, R.string.updates_check, R.string.download_confirmation_updates, result, null);
                }
            }
        }
    }

    /**
     * list offline data (only those types downloadable using the downloader)
     * and offer to remove it
     */
    public static void deleteOfflineData(final Activity activity) {
        // collect downloaded data
        final List<Pair<Integer, String>> offlineItems = new ArrayList<>();

        for (ContentStorage.FileInformation fi : ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS)) {
            if (!fi.isDirectory && StringUtils.endsWithIgnoreCase(fi.name, FileUtils.MAP_FILE_EXTENSION)) {
                offlineItems.add(new Pair<>(DOWNLOAD_TYPE_ALL_MAPS.id, fi.name));
            }
        }
        for (Download.DownloadTypeDescriptor type : Download.DownloadType.getOfflineMapThemeTypes()) {
            for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(type.type)) {
                offlineItems.add(new Pair<>(DOWNLOAD_TYPE_ALL_THEMES.id, offlineItem.localFile));
            }
        }
        for (ContentStorage.FileInformation fi : ContentStorage.get().list(PersistableFolder.BACKGROUND_MAPS)) {
            if (!fi.isDirectory && StringUtils.endsWithIgnoreCase(fi.name, FileUtils.BACKGROUND_MAP_FILE_EXTENSION)) {
                offlineItems.add(new Pair<>(DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id, fi.name));
            }
        }
        for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(DOWNLOADTYPE_BROUTER_TILES)) {
            offlineItems.add(new Pair<>(DOWNLOADTYPE_BROUTER_TILES.id, offlineItem.localFile));
        }
        for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(DOWNLOADTYPE_HILLSHADING_TILES)) {
            offlineItems.add(new Pair<>(DOWNLOADTYPE_HILLSHADING_TILES.id, offlineItem.localFile));
        }
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class).addOnSuccessListener(remoteModels -> {
            for (TranslateRemoteModel model : remoteModels) {
                if (!model.getLanguage().equalsIgnoreCase(OfflineTranslateUtils.LANGUAGE_UNDELETABLE)) {
                    offlineItems.add(new Pair<>(DOWNLOADTYPE_LANGUAGE_MODEL.id, model.getLanguage()));
                }
            }
            showDialog(activity, offlineItems);
        }).addOnFailureListener(e -> showDialog(activity, offlineItems));
    }

    private static void showDialog(final Activity activity, final List<Pair<Integer, String>> offlineItems) {
        // confirmation dialog (grouped by type)
        final SimpleDialog.ItemSelectModel<Pair<Integer, String>> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setButtonSelectionIsMandatory(false)
            .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
            .setItems(offlineItems)
            .setDisplayMapper((item, itemGroup) -> TextParam.text(item.second), (item, itemGroup) -> String.valueOf(item.first), null)
            .activateGrouping(item -> activity.getString(Download.DownloadType.getFromId(item.first).getTypeNameResId()))
            .setGroupDisplayMapper(gi -> TextParam.text("**" + gi.getGroup() + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true))
            .setGroupDisplayIconMapper(gi -> ImageParam.id(gi.getItems().isEmpty() ? 0 : Download.DownloadType.getFromId(gi.getItems().get(0).first).getIconResId()));

        SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_items))
            .setPositiveButton(TextParam.id(R.string.delete))
            .selectMultiple(model, (selected) -> {
                if (selected.isEmpty()) {
                    ActivityMixin.showShortToast(activity, R.string.no_files_selected);
                }
                int filesDeleted = 0;
                final ContentStorage cs = ContentStorage.get();
                for (Pair<Integer, String> offlineItem : selected) {
                    PersistableFolder folder = null;
                    if (offlineItem.first == DOWNLOAD_TYPE_ALL_MAPS.id) {
                        folder = PersistableFolder.OFFLINE_MAPS;
                    } else if (offlineItem.first == DOWNLOAD_TYPE_ALL_THEMES.id) {
                        folder = PersistableFolder.OFFLINE_MAP_THEMES;
                    } else if (offlineItem.first == DOWNLOADTYPE_BROUTER_TILES.id) {
                        folder = PersistableFolder.ROUTING_TILES;
                    } else if (offlineItem.first == DOWNLOADTYPE_HILLSHADING_TILES.id) {
                        folder = PersistableFolder.OFFLINE_MAP_SHADING;
                    } else if (offlineItem.first == DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id) {
                        folder = PersistableFolder.BACKGROUND_MAPS;
                    } else if (offlineItem.first == DOWNLOADTYPE_LANGUAGE_MODEL.id) {
                        OfflineTranslateUtils.deleteLanguageModel(offlineItem.second);
                        filesDeleted++;
                    }
                    if (folder != null) {
                        final List<ContentStorage.FileInformation> files = cs.list(folder);
                        for (ContentStorage.FileInformation fi : files) {
                            if (StringUtils.equals(fi.name, offlineItem.second)) {
                                cs.delete(fi.uri);
                            }
                        }
                        cs.delete(CompanionFileUtils.companionFileExists(files, offlineItem.second));
                        filesDeleted++;
                    }
                }
                ActivityMixin.showShortToast(activity, activity.getResources().getQuantityString(R.plurals.files_deleted, filesDeleted, filesDeleted));
                // update map lists in case something has changed there
                MapsforgeMapProvider.getInstance().updateOfflineMaps(); // update legacy NewMap/CGeoMap until they get removed
                TileProviderFactory.buildTileProviderList(true);
            });
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
                if (c == null) {
                    Log.w("checkPendingDownloads: querying DownloadManager returned null");
                    return;
                }
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

        final Data data = new Data.Builder()
                .putString(Intents.EXTRA_ADDRESS, downloadManager.getUriForDownloadedFile(id).toString())
                .putString(Intents.EXTRA_FILENAME, pendingDownload.getFilename())
                .putString(DownloaderUtils.RESULT_CHOSEN_URL, pendingDownload.getRemoteUrl())
                .putLong(DownloaderUtils.RESULT_DATE, pendingDownload.getDate())
                .putInt(DownloaderUtils.RESULT_TYPEID, pendingDownload.getOfflineMapTypeId())
                .build();
        final OneTimeWorkRequest copyJob = new OneTimeWorkRequest.Builder(ReceiveDownloadWorker.class)
                .setInputData(data)
                .build();
        WorkManager.getInstance(CgeoApplication.getInstance()).enqueue(copyJob);
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
                            sb.append(" - ").append(R.string.asdm_status_failed);
                        } else if (status == DownloadManager.STATUS_PAUSED) {
                            sb.append(" - ").append(R.string.asdm_status_paused);
                        } else if (status == DownloadManager.STATUS_PENDING) {
                            sb.append(" - ").append(R.string.asdm_status_pending);
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            sb.append(" - ").append(R.string.asdm_status_running);
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            sb.append(" - ").append(R.string.asdm_status_successful);
                        }
                    } else if (column == columnReason) {
                        final int reason = c.getInt(column);
                        sb.append(reason);
                        if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                            sb.append(" - ").append(R.string.asdm_paused_queued_for_wifi);
                        } else if (reason == DownloadManager.PAUSED_UNKNOWN) {
                            sb.append(" - ").append(R.string.asdm_paused_unknown);
                        } else if (reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) {
                            sb.append(" - ").append(R.string.asdm_paused_waiting_for_network);
                        } else if (reason == DownloadManager.PAUSED_WAITING_TO_RETRY) {
                            sb.append(" - ").append(R.string.asdm_paused_waiting_to_retry);
                        } else if (reason == DownloadManager.ERROR_CANNOT_RESUME) {
                            sb.append(" - ").append(R.string.asdm_error_cannot_resume);
                        } else if (reason == DownloadManager.ERROR_DEVICE_NOT_FOUND) {
                            sb.append(" - ").append(R.string.asdm_error_device_not_found);
                        } else if (reason == DownloadManager.ERROR_FILE_ALREADY_EXISTS) {
                            sb.append(" - ").append(R.string.asdm_error_file_already_exists);
                        } else if (reason == DownloadManager.ERROR_FILE_ERROR) {
                            sb.append(" - ").append(R.string.asdm_error_file_error);
                        } else if (reason == DownloadManager.ERROR_HTTP_DATA_ERROR) {
                            sb.append(" - ").append(R.string.asdm_error_http_data_error);
                        } else if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                            sb.append(" - ").append(R.string.asdm_error_insufficient_space);
                        } else if (reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
                            sb.append(" - ").append(R.string.asdm_error_too_many_redirects);
                        } else if (reason == DownloadManager.ERROR_UNHANDLED_HTTP_CODE) {
                            sb.append(" - ").append(R.string.asdm_error_unhandled_http_code);
                        } else if (reason == DownloadManager.ERROR_UNKNOWN) {
                            sb.append(" - ").append(R.string.asdm_error_unknown);
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
