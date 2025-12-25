// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.downloader

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.Intents
import cgeo.geocaching.MainActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.databinding.DownloaderConfirmationBinding
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider
import cgeo.geocaching.models.Download
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.permission.PermissionContext
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.storage.extension.PendingDownload
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.AsyncTaskWithProgressText
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.offlinetranslate.TranslationModelManager
import cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES
import cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_HILLSHADING_TILES
import cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_LANGUAGE_MODEL
import cgeo.geocaching.models.Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS
import cgeo.geocaching.models.Download.DownloadType.DOWNLOAD_TYPE_ALL_MAPS
import cgeo.geocaching.models.Download.DownloadType.DOWNLOAD_TYPE_ALL_THEMES

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.View
import android.widget.CheckBox
import android.content.Context.DOWNLOAD_SERVICE

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.util.Consumer
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections
import java.util.List

import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutableTriple

class DownloaderUtils {

    public static val RESULT_CHOSEN_URL: String = "chosenUrl"
    public static val RESULT_DATE: String = "dateInfo"
    public static val RESULT_TYPEID: String = "typeId"

    private DownloaderUtils() {
        // utility class
    }

    public static Boolean onOptionsItemSelected(final Activity activity, final Int id) {
        if (id == R.id.menu_download_offlinemap) {
            activity.startActivity(Intent(activity, DownloadSelectorActivity.class))
            return true
        } else if (id == R.id.menu_download_backgroundmap) {
            val intent: Intent = Intent(activity, DownloadSelectorActivity.class)
            intent.putExtra(DownloadSelectorActivity.INTENT_FIXED_DOWNLOADTYPE, Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id)
            activity.startActivity(intent)
            return true
        } else if (id == R.id.menu_delete_offline_data) {
            deleteOfflineData(activity)
            return true
        }
        return false
    }

    public static Unit checkForRoutingTileUpdates(final MainActivity activity) {
        if (Settings.useInternalRouting() && !PersistableFolder.ROUTING_TILES.isLegacy() && Settings.brouterAutoTileDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, R.id.tilesupdate, DOWNLOADTYPE_BROUTER_TILES, R.string.updates_check, R.string.tileupdate_info, DownloaderUtils::returnFromTileUpdateCheck)
        }
    }

    public static Unit returnFromTileUpdateCheck(final Boolean updateCheckAllowed) {
        Settings.setBrouterAutoTileDownloadsLastCheck(!updateCheckAllowed)
    }

    public static Unit checkForMapUpdates(final MainActivity activity) {
        if (Settings.mapAutoDownloadsNeedUpdate()) {
            DownloaderUtils.checkForUpdatesAndDownloadAll(activity, R.id.mapupdate, Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED, R.string.updates_check, R.string.mapupdate_info, DownloaderUtils::returnFromMapUpdateCheck)
        }
    }

    public static Unit returnFromMapUpdateCheck(final Boolean updateCheckAllowed) {
        Settings.setMapAutoDownloadsLastCheck(!updateCheckAllowed)
    }

    private static String getFilenameFromUri(final Uri uri) {
        String temp = uri.getLastPathSegment()
        if (temp == null) {
            temp = "default.map"
        }
        return temp
    }

    public static Unit triggerDownload(final Activity activity, @StringRes final Int title, final Int type, final Uri uri, final String additionalInfo, final String sizeInfo, final Runnable dialogDismissedCallback, final Consumer<Long> downloadStartedCallback) {
        val filename: String = getFilenameFromUri(uri)
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(title)
        val binding: DownloaderConfirmationBinding = DownloaderConfirmationBinding.inflate(activity.getLayoutInflater())
        builder.setView(binding.getRoot())
        binding.downloadInfo1.setText(String.format(activity.getString(R.string.download_confirmation), StringUtils.isNotBlank(additionalInfo) ? additionalInfo + "\n\n" : "", filename, "\n\n" + activity.getString(R.string.download_warning) + (StringUtils.isNotBlank(sizeInfo) ? "\n\n" + sizeInfo : "")))
        binding.downloadInfo2.setVisibility(View.GONE)

        builder
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    val allowMeteredNetwork: Boolean = binding.allowMeteredNetwork.isChecked()
                    val downloadManager: DownloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE)
                    if (null != downloadManager) {
                        val id: Long = addDownload(activity, downloadManager, type, uri, filename, allowMeteredNetwork)
                        if (id != -1) {
                            if (downloadStartedCallback != null) {
                                downloadStartedCallback.accept(id)
                            }

                            // check for required extra files (e. g.: map theme)
                            val downloader: AbstractDownloader = Download.DownloadType.getInstance(type)
                            if (downloader != null) {
                                val extraFile: DownloadDescriptor = downloader.getExtrafile(activity, uri)
                                if (extraFile != null) {
                                    addDownload(activity, downloadManager, extraFile.type, extraFile.uri, extraFile.filename, allowMeteredNetwork)
                                }
                            }
                            ActivityMixin.showShortToast(activity, R.string.download_started)
                        } else {
                            ActivityMixin.showShortToast(activity, R.string.download_enqueing_error)
                        }
                    } else {
                        ActivityMixin.showToast(activity, R.string.downloadmanager_not_available)
                    }
                    dialog.dismiss()
                    if (dialogDismissedCallback != null) {
                        dialogDismissedCallback.run()
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    dialog.dismiss()
                    if (dialogDismissedCallback != null) {
                        dialogDismissedCallback.run()
                    }
                })
                .create()
                .show()
    }

    @SuppressLint("SetTextI18n")
    public static Unit triggerDownloads(final Activity activity, @StringRes final Int title, @StringRes final Int confirmation, final List<Download> downloads, final Action1<Boolean> downloadTriggered) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(activity)
        builder.setTitle(title)
        val binding: DownloaderConfirmationBinding = DownloaderConfirmationBinding.inflate(activity.getLayoutInflater())
        builder.setView(binding.getRoot())
        binding.downloadInfo1.setText(confirmation)
        binding.downloadInfo2.setText(R.string.download_warning)

        for (Download download : downloads) {
            val cb: CheckBox = CheckBox(ContextThemeWrapper(activity, R.style.checkbox_full))
            val sizeinfo: String = download.getSizeInfo()
            cb.setText(download.getName() + (StringUtils.isNotBlank(sizeinfo) ? " (" + sizeinfo + ")" : ""))
            cb.setChecked(true)
            download.customMarker = true

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                cb.setChecked(isChecked)
                download.customMarker = isChecked
                Log.i(download.getName() + ", checked=" + isChecked)
            })
            binding.checkboxPlaceholder.addView(cb)
        }

        builder
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    val allowMeteredNetwork: Boolean = binding.allowMeteredNetwork.isChecked()

                    val downloadManager: DownloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE)
                    if (null != downloadManager) {
                        Int numFiles = 0
                        for (Download download : downloads) {
                            if (download.customMarker) {
                                numFiles++
                                final DownloadManager.Request request = DownloadManager.Request(download.getUri())
                                        .setTitle(download.getName())
                                        .setDescription(String.format(activity.getString(R.string.downloadmap_filename), download.getName()))
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.getName())
                                        .setAllowedOverMetered(allowMeteredNetwork)
                                        .setAllowedOverRoaming(allowMeteredNetwork)
                                Log.i("Download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + download.getName())
                                AndroidRxUtils.networkScheduler.scheduleDirect(() -> PendingDownload.add(downloadManager.enqueue(request), download.getName(), download.getUri().toString(), download.getDateInfo(), download.getType().id))
                            }
                        }
                        ActivityMixin.showShortToast(activity, numFiles > 0 ? R.string.download_started : R.string.no_files_selected)
                        if (downloadTriggered != null) {
                            downloadTriggered.call(true)
                        }
                    } else {
                        ActivityMixin.showToast(activity, R.string.downloadmanager_not_available)
                    }
                    dialog.dismiss()
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (downloadTriggered != null) {
                        downloadTriggered.call(false)
                    }
                    dialog.dismiss()
                })
                .create()
                .show()
    }

    private static Long addDownload(final Activity activity, final DownloadManager downloadManager, final Int type, final Uri uri, final String filename, final Boolean allowMeteredNetwork) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !PermissionContext.LEGACY_WRITE_EXTERNAL_STORAGE.hasAllPermissions()) {
            // those versions still need WRITE_EXTERNAL_STORAGE permission to enqueue a download
            SimpleDialog.ofContext(activity).setTitle(TextParam.id(R.string.permission_missing)).setMessage(TextParam.id(R.string.storage_permission_needed)).show()
            return -1
        }
        final DownloadManager.Request request = DownloadManager.Request(uri)
                .setTitle(filename)
                .setDescription(String.format(activity.getString(R.string.downloadmap_filename), filename))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                .setAllowedOverMetered(allowMeteredNetwork)
                .setAllowedOverRoaming(allowMeteredNetwork)
        Log.i("Download enqueued: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename)
        val id: Long = downloadManager.enqueue(request)
        PendingDownload.add(id, filename, uri.toString(), System.currentTimeMillis(), type)
        return id
    }

    public static class DownloadDescriptor {
        public final String filename
        public final Uri uri
        public final Int type

        DownloadDescriptor(final String filename, final Uri uri, final Int type) {
            this.filename = filename
            this.uri = uri
            this.type = type
        }
    }

    interface DirectoryWritable {
        Unit run(PersistableFolder folder, Boolean isAvailable)
    }

    public static Unit checkTargetDirectory(final Activity activity, final PersistableFolder folder, final Boolean beforeDownload, final DirectoryWritable callback) {
        val mapDirIsReady: Boolean = ContentStorage.get().ensureFolder(folder)

        if (mapDirIsReady) {
            callback.run(folder, true)
        } else if (beforeDownload) {
            SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).setPositiveButton(TextParam.id(R.string.button_continue)).confirm(
                    () -> callback.run(folder, true), () -> callback.run(folder, false))
        } else {
            SimpleDialog.of(activity).setTitle(R.string.download_title).setMessage(R.string.downloadmap_target_not_writable, folder).show(() -> callback.run(folder, false))
        }
    }

    /**
     * displays an info to the user to check for updates
     * if button pressed: checks whether updates are available for the type specified
     * if yes: ask user to download them all
     * if yes: trigger download(s)
     */
    public static Unit checkForUpdatesAndDownloadAll(final MainActivity activity, final Int layout, final Download.DownloadType type, @StringRes final Int title, @StringRes final Int info, final Action1<Boolean> callback) {
        activity.displayActionItem(layout, info, true, (actionRequested) -> {
            if (actionRequested) {
                CheckForDownloadsTask(activity, title, type).execute()
            }
            callback.call(actionRequested)
        })
    }

    // execute download check
    public static Unit checkForUpdatesAndDownloadAll(final Activity activity, final Download.DownloadType type, @StringRes final Int title, final Action1<Boolean> callback) {
        CheckForDownloadsTask(activity, title, type).execute()
        callback.call(true)
    }

    private static class CheckForDownloadsTask : AsyncTaskWithProgressText()<Void, List<Download>> {
        private final Download.DownloadType currentType
        private var lastUrl: String = ""
        private var lastPage: String = ""
        private final WeakReference<Activity> activityRef

        CheckForDownloadsTask(final Activity activity, @StringRes final Int title, final Download.DownloadType type) {
            super(activity, activity.getString(title), activity.getString(R.string.downloadmap_checking_for_updates))
            this.activityRef = WeakReference<>(activity)
            this.currentType = type
        }

        override         protected List<Download> doInBackgroundInternal(final Void[] none) {
            val result: List<Download> = ArrayList<>()
            val existingFiles: ArrayList<CompanionFileUtils.DownloadedFileData> = ArrayList<>()
            if (currentType == (Download.DownloadType.DOWNLOADTYPE_ALL_MAPRELATED)) {
                val typeDescriptors: ArrayList<Download.DownloadTypeDescriptor> = Download.DownloadType.getOfflineAllMapRelatedTypes()
                for (Download.DownloadTypeDescriptor typeDescriptor : typeDescriptors) {
                    existingFiles.addAll(CompanionFileUtils.availableOfflineMaps(typeDescriptor.type))
                }
            } else {
                existingFiles.addAll(CompanionFileUtils.availableOfflineMaps(currentType))
            }

            for (CompanionFileUtils.DownloadedFileData existingFile : existingFiles) {
                val download: Download = checkForUpdate(existingFile)
                if (download != null && download.getDateInfo() > existingFile.remoteDate) {
                    download.setAddInfo(CalendarUtils.yearMonthDay(existingFile.remoteDate))
                    result.add(download)
                }
            }
            return result
        }

        @WorkerThread
        private Download checkForUpdate(final CompanionFileUtils.DownloadedFileData offlineMapData) {
            val downloader: AbstractDownloader = Download.DownloadType.getInstance(offlineMapData.remoteParsetype)
            if (downloader == null) {
                Log.e("Map update checker: Cannot find map downloader of type " + offlineMapData.remoteParsetype + " for file " + offlineMapData.localFile)
                return null
            }

            val url: String = downloader.getUpdatePageUrl(offlineMapData.remotePage)
            final String page

            if (url == (lastUrl)) {
                page = lastPage
            } else {
                val params: Parameters = Parameters()
                try {
                    val response: Response = Network.getRequest(url, params).blockingGet()
                    page = Network.getResponseData(response, true)

                    if (StringUtils.isBlank(page)) {
                        Log.e("getMap: No data from server")
                        return null
                    }

                    lastUrl = url
                    lastPage = page
                } catch (final Exception e) {
                    return null
                }
            }

            try {
                return downloader.checkUpdateFor(page, offlineMapData.remotePage, offlineMapData.remoteFile)
            } catch (final Exception e) {
                Log.e("Map update checker: error parsing parsing html page", e)
                return null
            }
        }

        override         protected Unit onPostExecuteInternal(final List<Download> result) {
            val activity: Activity = activityRef.get()
            if (activity != null) {
                if (result.isEmpty()) {
                    ViewUtils.showShortToast(activity, R.string.no_updates_found)
                } else {
                    triggerDownloads(activity, R.string.updates_check, R.string.download_confirmation_updates, result, null)
                }
            }
        }
    }

    /**
     * list offline data (only those types downloadable using the downloader)
     * and offer to remove it
     */
    public static Unit deleteOfflineData(final Activity activity) {
        // collect downloaded data
        final List<ImmutableTriple<Integer, String, CharSequence>> offlineItems = ArrayList<>()

        for (ContentStorage.FileInformation fi : ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS)) {
            if (!fi.isDirectory && StringUtils.endsWithIgnoreCase(fi.name, FileUtils.MAP_FILE_EXTENSION)) {
                offlineItems.add(ImmutableTriple<>(DOWNLOAD_TYPE_ALL_MAPS.id, fi.name, fi.name))
            }
        }
        for (Download.DownloadTypeDescriptor type : Download.DownloadType.getOfflineMapThemeTypes()) {
            for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(type.type)) {
                offlineItems.add(ImmutableTriple<>(DOWNLOAD_TYPE_ALL_THEMES.id, offlineItem.localFile, offlineItem.localFile))
            }
        }
        for (ContentStorage.FileInformation fi : ContentStorage.get().list(PersistableFolder.BACKGROUND_MAPS)) {
            if (!fi.isDirectory && StringUtils.endsWithIgnoreCase(fi.name, FileUtils.BACKGROUND_MAP_FILE_EXTENSION)) {
                offlineItems.add(ImmutableTriple<>(DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id, fi.name, fi.name))
            }
        }
        for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(DOWNLOADTYPE_BROUTER_TILES)) {
            offlineItems.add(ImmutableTriple<>(DOWNLOADTYPE_BROUTER_TILES.id, offlineItem.localFile, offlineItem.localFile))
        }
        for (CompanionFileUtils.DownloadedFileData offlineItem : CompanionFileUtils.availableOfflineMaps(DOWNLOADTYPE_HILLSHADING_TILES)) {
            offlineItems.add(ImmutableTriple<>(DOWNLOADTYPE_HILLSHADING_TILES.id, offlineItem.localFile, offlineItem.localFile))
        }

        for (String candidate : TranslationModelManager.get().getSupportedLanguages()) {
            if (!"en" == (candidate) && TranslationModelManager.get().isAvailable(candidate)) {
                offlineItems.add(ImmutableTriple<>(DOWNLOADTYPE_LANGUAGE_MODEL.id, candidate, LocalizationUtils.getLocaleDisplayName(candidate, false, true)))
            }
        }
        Collections.sort(offlineItems, (left, right) -> TextUtils.COLLATOR.compare(left.getRight(), right.getRight()))
        showDialog(activity, offlineItems)
    }

    private static Unit showDialog(final Activity activity, final List<ImmutableTriple<Integer, String, CharSequence>> offlineItems) {
        // confirmation dialog (grouped by type)
        final SimpleDialog.ItemSelectModel<ImmutableTriple<Integer, String, CharSequence>> model = SimpleDialog.ItemSelectModel<>()
        model
                .setButtonSelectionIsMandatory(false)
                .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
                .setItems(offlineItems)
                .setDisplayMapper((item, itemGroup) -> TextParam.text(item.right), (item, itemGroup) -> String.valueOf(item.left), null)
                .activateGrouping(item -> activity.getString(Download.DownloadType.getFromId(item.left).getTypeNameResId()))
                .setGroupDisplayMapper(gi -> TextParam.text("**" + gi.getGroup() + "** *(" + gi.getContainedItemCount() + ")*").setMarkdown(true))
                .setGroupDisplayIconMapper(gi -> ImageParam.id(gi.getItems().isEmpty() ? 0 : Download.DownloadType.getFromId(gi.getItems().get(0).left).getIconResId()))

        SimpleDialog.of(activity).setTitle(TextParam.id(R.string.delete_items))
                .setPositiveButton(TextParam.id(R.string.delete))
                .selectMultiple(model, (selected) -> {
                    if (selected.isEmpty()) {
                        ActivityMixin.showShortToast(activity, R.string.no_files_selected)
                    }
                    Int filesDeleted = 0
                    val cs: ContentStorage = ContentStorage.get()
                    for (ImmutableTriple<Integer, String, CharSequence> offlineItem : selected) {
                        PersistableFolder folder = null
                        if (offlineItem.left == DOWNLOAD_TYPE_ALL_MAPS.id) {
                            folder = PersistableFolder.OFFLINE_MAPS
                        } else if (offlineItem.left == DOWNLOAD_TYPE_ALL_THEMES.id) {
                            folder = PersistableFolder.OFFLINE_MAP_THEMES
                        } else if (offlineItem.left == DOWNLOADTYPE_BROUTER_TILES.id) {
                            folder = PersistableFolder.ROUTING_TILES
                        } else if (offlineItem.left == DOWNLOADTYPE_HILLSHADING_TILES.id) {
                            folder = PersistableFolder.OFFLINE_MAP_SHADING
                        } else if (offlineItem.left == DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS.id) {
                            folder = PersistableFolder.BACKGROUND_MAPS
                        } else if (offlineItem.left == DOWNLOADTYPE_LANGUAGE_MODEL.id) {
                            TranslationModelManager.get().deleteLanguage(offlineItem.middle)
                            filesDeleted++
                        }
                        if (folder != null) {
                            val files: List<ContentStorage.FileInformation> = cs.list(folder)
                            for (ContentStorage.FileInformation fi : files) {
                                if (StringUtils == (fi.name, offlineItem.middle)) {
                                    cs.delete(fi.uri)
                                }
                            }
                            cs.delete(CompanionFileUtils.companionFileExists(files, offlineItem.middle))
                            filesDeleted++
                        }
                    }
                    ActivityMixin.showShortToast(activity, activity.getResources().getQuantityString(R.plurals.files_deleted, filesDeleted, filesDeleted))
                    // update map lists in case something has changed there
                    MapsforgeMapProvider.getInstance().updateOfflineMaps(); // update legacy NewMap/CGeoMap until they get removed
                    TileProviderFactory.buildTileProviderList(true)
                })
    }

    /**
     * check for successfully completed, but not yet received downloads
     * and offer to download them
     */
    public static Unit checkPendingDownloads(final Activity activity) {
        val downloadManager: DownloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE)
        if (null != downloadManager) {
            final DownloadManager.Query query = DownloadManager.Query()
            try (Cursor c = downloadManager.query(query)) {
                if (c == null) {
                    Log.w("checkPendingDownloads: querying DownloadManager returned null")
                    return
                }
                val columnId: Int = c.getColumnIndex(DownloadManager.COLUMN_ID)
                val columnStatus: Int = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val columnBytesA: Int = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val columnBytesT: Int = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                while (c.moveToNext()) {
                    val status: Int = c.getInt(columnStatus)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val bytesA: Int = c.getInt(columnBytesA)
                        val bytesT: Int = c.getInt(columnBytesT)
                        if (bytesA == bytesT) {
                            val id: Long = c.getLong(columnId)
                            val p: PendingDownload = PendingDownload.load(id)
                            if (p != null) {
                                startReceive(activity, downloadManager, id, p)
                                Log.d("download #" + id + " retriggered manually")
                            }
                        }
                    }
                }
            }
        }
    }

    public static Unit startReceive(final Context context, final DownloadManager downloadManager, final Long id, final PendingDownload pendingDownload) {
        PendingDownload.remove(id)

        val data: Data = Data.Builder()
                .putString(Intents.EXTRA_ADDRESS, downloadManager.getUriForDownloadedFile(id).toString())
                .putString(Intents.EXTRA_FILENAME, pendingDownload.getFilename())
                .putString(DownloaderUtils.RESULT_CHOSEN_URL, pendingDownload.getRemoteUrl())
                .putLong(DownloaderUtils.RESULT_DATE, pendingDownload.getDate())
                .putInt(DownloaderUtils.RESULT_TYPEID, pendingDownload.getOfflineMapTypeId())
                .build()
        val copyJob: OneTimeWorkRequest = OneTimeWorkRequest.Builder(ReceiveDownloadWorker.class)
                .setInputData(data)
                .build()
        WorkManager.getInstance(CgeoApplication.getInstance()).enqueue(copyJob)
    }

    public static Unit dumpDownloadmanagerInfos(final Activity activity) {
        val downloadManager: DownloadManager = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE)
        final DownloadManager.Query query = DownloadManager.Query()
        val sb: StringBuilder = StringBuilder()
        try (Cursor c = downloadManager.query(query)) {
            val columnStatus: Int = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val columnReason: Int = c.getColumnIndex(DownloadManager.COLUMN_REASON)
            final Int[] columns = {
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
            }
            while (c.moveToNext()) {
                for (Int column : columns) {
                    sb.append("- ").append(c.getColumnName(column)).append(" = ")
                    if (column == columnStatus) {
                        sb.append(c.getString(column))
                        val status: Int = c.getInt(column)
                        if (status == DownloadManager.STATUS_FAILED) {
                            sb.append(" - ").append(R.string.asdm_status_failed)
                        } else if (status == DownloadManager.STATUS_PAUSED) {
                            sb.append(" - ").append(R.string.asdm_status_paused)
                        } else if (status == DownloadManager.STATUS_PENDING) {
                            sb.append(" - ").append(R.string.asdm_status_pending)
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            sb.append(" - ").append(R.string.asdm_status_running)
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            sb.append(" - ").append(R.string.asdm_status_successful)
                        }
                    } else if (column == columnReason) {
                        val reason: Int = c.getInt(column)
                        sb.append(reason)
                        if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                            sb.append(" - ").append(R.string.asdm_paused_queued_for_wifi)
                        } else if (reason == DownloadManager.PAUSED_UNKNOWN) {
                            sb.append(" - ").append(R.string.asdm_paused_unknown)
                        } else if (reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) {
                            sb.append(" - ").append(R.string.asdm_paused_waiting_for_network)
                        } else if (reason == DownloadManager.PAUSED_WAITING_TO_RETRY) {
                            sb.append(" - ").append(R.string.asdm_paused_waiting_to_retry)
                        } else if (reason == DownloadManager.ERROR_CANNOT_RESUME) {
                            sb.append(" - ").append(R.string.asdm_error_cannot_resume)
                        } else if (reason == DownloadManager.ERROR_DEVICE_NOT_FOUND) {
                            sb.append(" - ").append(R.string.asdm_error_device_not_found)
                        } else if (reason == DownloadManager.ERROR_FILE_ALREADY_EXISTS) {
                            sb.append(" - ").append(R.string.asdm_error_file_already_exists)
                        } else if (reason == DownloadManager.ERROR_FILE_ERROR) {
                            sb.append(" - ").append(R.string.asdm_error_file_error)
                        } else if (reason == DownloadManager.ERROR_HTTP_DATA_ERROR) {
                            sb.append(" - ").append(R.string.asdm_error_http_data_error)
                        } else if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
                            sb.append(" - ").append(R.string.asdm_error_insufficient_space)
                        } else if (reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS) {
                            sb.append(" - ").append(R.string.asdm_error_too_many_redirects)
                        } else if (reason == DownloadManager.ERROR_UNHANDLED_HTTP_CODE) {
                            sb.append(" - ").append(R.string.asdm_error_unhandled_http_code)
                        } else if (reason == DownloadManager.ERROR_UNKNOWN) {
                            sb.append(" - ").append(R.string.asdm_error_unknown)
                        }

                    } else {
                        sb.append(c.getString(column))
                    }
                    sb.append("\n")
                }
                sb.append("\n---\n\n")
            }
        }

        SimpleDialog.of(activity).setTitle(R.string.debug_current_downloads).setMessage(TextParam.text(sb.toString()).setMarkdown(true)).show()
    }

}
