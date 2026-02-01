package cgeo.geocaching.unifiedmap.layers;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.Folder;
import cgeo.geocaching.storage.FolderUtils;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Synchronizes MBTiles files from the public maps/_mbtiles folder to the app's internal media directory.
 * This allows users to place MBTiles files in a public folder and have them automatically synced and
 * companion files generated.
 */
public final class MBTilesSynchronizer {

    private static final PersistableFolder PUBLIC_MBTILES_FOLDER = PersistableFolder.PUBLIC_MBTILES;
    private static final long FILESYNC_MAX_FILESIZE = 500 * 1024 * 1024; // 500MB max file size for sync

    private static MBTilesSyncTask syncTask = null;

    private MBTilesSynchronizer() {
        // utility class
    }

    /**
     * Returns true if MBTiles folder synchronization is currently active/enabled
     */
    public static boolean isSynchronizationActive() {
        return Settings.getSyncMBTilesFolder();
    }

    /**
     * Depending on whether MBTiles folder synchronization is currently turned off or on, this
     * method does two different things:
     * * if turned off: app-private media folder content from sync is safely deleted
     * * if turned on: folder is re-synced (every change in source folder is synced to target folder)
     * <br>
     * In any case, this method will take care of thread synchronization e.g. when a sync is currently running then
     * this sync will first be aborted, and only after that either target folder is deleted (if sync=off) or sync is restarted (if sync=on)
     * <br>
     * Call this method whenever you feel that there might be a change in MBTiles files in
     * public folder. Sync will be done in background task and reports its progress via toasts
     */
    public static void resynchronizeOrDeleteMBTilesFolder() {
        MBTilesSyncTask.requestResynchronization(
                PUBLIC_MBTILES_FOLDER.getFolder(),
                LocalStorage.getMBTilesInternalSyncDir(),
                isSynchronizationActive()
        );
    }

    private static class MBTilesSyncTask extends AsyncTask<Void, Void, FolderUtils.FolderProcessResult> {

        private enum AfterSyncRequest { EXIT_NORMAL, REDO, ABORT_DELETE }

        private static final Object syncTaskMutex = new Object();

        private final Folder source;
        private final File target;
        private final boolean doSync;

        private final AtomicBoolean cancelFlag = new AtomicBoolean(false);

        private final Object requestRedoMutex = new Object();
        private boolean taskIsDone = false;
        private AfterSyncRequest afterSyncRequest = AfterSyncRequest.EXIT_NORMAL;
        private long startTime = System.currentTimeMillis();

        public static void requestResynchronization(final Folder source, final File target, final boolean doSync) {
            synchronized (syncTaskMutex) {
                if (syncTask == null || !syncTask.requestAfter(doSync ? MBTilesSyncTask.AfterSyncRequest.REDO : MBTilesSyncTask.AfterSyncRequest.ABORT_DELETE)) {
                    Log.i("[MBTilesFolderSync] start synchronization " + source + " -> " + target);
                    syncTask = new MBTilesSyncTask(source, target, doSync);
                    syncTask.execute();
                }
            }
        }

        private MBTilesSyncTask(final Folder source, final File target, final boolean doSync) {
            this.source = source;
            this.target = target;
            this.doSync = doSync;
        }

        /**
         * Requests for a running task to redo sync after finished. May fail if task is already done, but in this case the task may safely be discarded
         */
        public boolean requestAfter(final AfterSyncRequest afterSyncRequest) {
            synchronized (requestRedoMutex) {
                if (taskIsDone || !doSync) {
                    return false;
                }
                Log.i("[MBTilesFolderSync] Requesting '" + afterSyncRequest + "' " + source + " -> " + target);
                cancelFlag.set(true);
                this.afterSyncRequest = afterSyncRequest;
                startTime = System.currentTimeMillis();
                return true;
            }
        }

        @Override
        protected FolderUtils.FolderProcessResult doInBackground(final Void[] params) {
            Log.i("[MBTilesFolderSync] start synchronization " + source + " -> " + target + " (doSync=" + doSync + ")");
            FolderUtils.FolderProcessResult result = null;
            if (!doSync) {
                FileUtils.deleteDirectory(target);
            } else {
                boolean cont = true;
                while (cont) {
                    result = FolderUtils.get().synchronizeFolder(source, target, MBTilesSyncTask::shouldBeSynced, cancelFlag, null);
                    synchronized (requestRedoMutex) {
                        switch (afterSyncRequest) {
                            case EXIT_NORMAL:
                                taskIsDone = true;
                                cont = false;
                                generateCompanionFiles();
                                break;
                            case ABORT_DELETE:
                                FileUtils.deleteDirectory(target);
                                cont = false;
                                break;
                            case REDO:
                                Log.i("[MBTilesFolderSync] redo synchronization " + source + " -> " + target);
                                cancelFlag.set(false);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(final FolderUtils.FolderProcessResult result) {
            Log.i("[MBTilesFolderSync] Finished synchronization (state=" + afterSyncRequest + ")");
            //show toast only if something actually happened
            if (result != null && result.filesModified > 0) {
                showToast(R.string.mbtiles_foldersync_finished_toast,
                        LocalizationUtils.getString(R.string.persistablefolder_public_mbtiles),
                        Formatter.formatDuration(System.currentTimeMillis() - startTime),
                        result.filesModified, LocalizationUtils.getPlural(R.plurals.file_count, result.filesInSource, "file(s)"));
            }
            Log.i("[MBTilesFolderSync] Finished synchronization callback");
        }

        private static boolean shouldBeSynced(final ContentStorage.FileInformation fileInfo) {
            return fileInfo != null 
                    && fileInfo.name.endsWith(FileUtils.BACKGROUND_MAP_FILE_EXTENSION) 
                    && fileInfo.size <= FILESYNC_MAX_FILESIZE;
        }

        /**
         * Generate companion files for all synced MBTiles files
         */
        private void generateCompanionFiles() {
            final File[] files = target.listFiles((dir, name) -> name.endsWith(FileUtils.BACKGROUND_MAP_FILE_EXTENSION));
            if (files == null) {
                return;
            }

            for (File file : files) {
                generateCompanionFile(file);
            }
        }

        /**
         * Generate a companion file for a single MBTiles file
         */
        private void generateCompanionFile(@NonNull final File mbtilesFile) {
            final String filename = mbtilesFile.getName();
            final File companionFile = new File(mbtilesFile.getParentFile(), filename + CompanionFileUtils.INFOFILE_SUFFIX);

            // Only generate if companion file doesn't already exist
            if (companionFile.exists()) {
                return;
            }

            try (java.io.FileOutputStream output = new java.io.FileOutputStream(companionFile)) {
                // Generate a display name from the filename
                final String baseName = filename.substring(0, filename.length() - FileUtils.BACKGROUND_MAP_FILE_EXTENSION.length());
                final String displayName = CompanionFileUtils.getDisplayName(baseName);

                final java.util.Properties prop = new java.util.Properties();
                prop.setProperty("local.file", filename);
                prop.setProperty("displayname", displayName);
                prop.setProperty("remote.date", cgeo.geocaching.utils.CalendarUtils.yearMonthDay(mbtilesFile.lastModified()));
                prop.setProperty("remote.page", "");
                prop.setProperty("remote.file", filename);
                prop.setProperty("remote.parsetype", "-1"); // Use -1 to indicate user-synced file

                prop.store(output, "MBTiles file info - synced from public folder. Edit displayname to change name in list. Charset is ISO-8859-1, use \\u#### for Unicode characters");

                Log.i("[MBTilesFolderSync] Generated companion file for " + filename);
            } catch (Exception e) {
                Log.w("[MBTilesFolderSync] Failed to generate companion file for " + filename, e);
            }
        }

        private static void showToast(final int resId, final Object... params) {
            final ImmutablePair<String, String> msgs = LocalizationUtils.getMultiPurposeString(resId, "MBTiles", params);
            ActivityMixin.showApplicationToast(msgs.left);
            Log.iForce("[MBTilesSynchronizer]" + msgs.right);
        }
    }
}
