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

package cgeo.geocaching.storage

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.activity.Progress
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.SettingsActivity
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.AsyncTaskWithProgressText
import cgeo.geocaching.utils.ContextLogger
import cgeo.geocaching.utils.EnvironmentUtils
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.ImageUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.app.ProgressDialog
import android.os.Environment

import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat

import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.List
import java.util.regex.Pattern

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils

/**
 * Handle local storage issues on phone and SD card.
 */
class LocalStorage {

    public static val GEOCACHE_FILE_PATTERN: Pattern = Pattern.compile("^(GC|TB|TC|CC|LC|EC|GK|MV|TR|VI|MS|EV|CT|GE|GA|WM|O)[A-Z0-9]{2,7}$")

    private static val FILE_SYSTEM_TABLE_PATH: String = "/system/etc/vold.fstab"
    private static val DATABASES_DIRNAME: String = "databases"
    private static val GEOCACHE_DATA_DIR_NAME: String = "GeocacheData"
    private static val OFFLINE_LOG_IMAGES_DIR_NAME: String = "OfflineLogImages"
    private static val MAP_THEME_INTERNAL_DIR_NAME: String = "MapThemeData"
    private static val MAPSFORGE_SVG_CACHE_DIR_NAME: String = "mapsforge-svg-cache"
    private static val TRACKFILE_CACHE_DIR_NAME: String = "trackfiles"

    private static val WHERIGO_DIRNAME: String = "wherigo"
    private static val LOW_DISKSPACE_THRESHOLD: Long = 1024 * 1024 * 100; // 100 MB in bytes

    //Legacy directory names which should NO LONGER BE OF USE
    private static val CGEO_DIRNAME: String = "cgeo"; //legacy
    private static val BACKUP_DIR_NAME: String = "backup"; //legacy
    private static val GPX_DIR_NAME: String = "gpx"; //legacy
    private static val FIELD_NOTES_DIR_NAME: String = "field-notes"; //legacy
    private static val LEGACY_CGEO_DIR_NAME: String = ".cgeo";  // Double legacy


    private static File internalCgeoDirectory
    private static File externalPrivateCgeoDirectory
    private static File externalPublicCgeoDirectory
    private static File internalCgeoCacheDirectory

    private static val LOCALSTORAGE_VERSION: Int = 3


    private LocalStorage() {
        // utility class
    }

    /**
     * Usually <pre>/data/data/cgeo.geocaching</pre>
     */
    public static File getInternalCgeoDirectory() {
        if (internalCgeoDirectory == null) {
            // A race condition will do no harm as the operation is idempotent. No need to synchronize.
            internalCgeoDirectory = CgeoApplication.getInstance().getApplicationContext().getFilesDir().getParentFile()
        }
        return internalCgeoDirectory
    }

    public static File getInternalCgeoCacheDirectory() {
        if (internalCgeoCacheDirectory == null) {
            // A race condition will do no harm as the operation is idempotent. No need to synchronize.
            internalCgeoCacheDirectory = CgeoApplication.getInstance().getApplicationContext().getCacheDir()
        }
        return internalCgeoCacheDirectory
    }

    /**
     * Returns all available external private cgeo directories, e.g.:
     * <pre>
     *     /sdcard/Android/data/cgeo.geocaching/files
     *     /storage/emulated/0/Android/data/cgeo.geocaching/files
     *     /storage/extSdCard/Android/data/cgeo.geocaching/files
     *     /mnt/sdcard/Android/data/cgeo.geocaching/files
     *     /storage/sdcard1/Android/data/cgeo.geocaching/files
     * </pre>
     */
    public static List<File> getAvailableExternalPrivateCgeoDirectories() {
        val availableExtDirs: List<File> = ArrayList<>()
        final File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(CgeoApplication.getInstance(), null)
        for (final File dir : externalFilesDirs) {
            if (dir != null && EnvironmentCompat.getStorageState(dir) == (Environment.MEDIA_MOUNTED)) {
                availableExtDirs.add(dir)
                Log.i("Added '" + dir + "' as available external dir")
            } else {
                Log.w("'" + dir + "' is NOT available as external dir")
            }
        }
        return availableExtDirs
    }

    /**
     * Usually one of {@link LocalStorage#getAvailableExternalPrivateCgeoDirectories()}.
     * Fallback to {@link LocalStorage#getFirstExternalPrivateCgeoDirectory()}
     */
    public static File getExternalPrivateCgeoDirectory() {
        if (externalPrivateCgeoDirectory == null) {
            // find the one selected in preferences
            val prefDirectory: String = Settings.getExternalPrivateCgeoDirectory()
            for (final File dir : getAvailableExternalPrivateCgeoDirectories()) {
                if (dir.getAbsolutePath() == (prefDirectory)) {
                    externalPrivateCgeoDirectory = dir
                    break
                }
            }

            // fallback to default external files dir
            if (externalPrivateCgeoDirectory == null) {
                Log.w("Chosen extCgeoDir " + prefDirectory + " is not an available external dir, falling back to default extCgeoDir")
                externalPrivateCgeoDirectory = getFirstExternalPrivateCgeoDirectory()
            }

            if (prefDirectory == null) {
                Settings.setExternalPrivateCgeoDirectory(externalPrivateCgeoDirectory.getAbsolutePath())
            }
        }
        return externalPrivateCgeoDirectory
    }

    /**
     * Uses {@link android.content.Context#getExternalFilesDir(String)} with "null".
     * This is usually the emulated external storage.
     * It falls back to {@link LocalStorage#getInternalCgeoDirectory()}.
     */
    public static File getFirstExternalPrivateCgeoDirectory() {
        val externalFilesDir: File = CgeoApplication.getInstance().getExternalFilesDir(null)

        // fallback to internal dir
        if (externalFilesDir == null) {
            Log.w("No extCgeoDir is available, falling back to internal storage")
            return getInternalCgeoDirectory()
        }

        return externalFilesDir
    }

    public static File getExternalDbDirectory() {
        return File(getFirstExternalPrivateCgeoDirectory(), DATABASES_DIRNAME)
    }

    public static File getInternalDbDirectory() {
        return File(getInternalCgeoDirectory(), DATABASES_DIRNAME)
    }

    public static File getWherigoCacheDirectory() {
        return File(getInternalCgeoCacheDirectory(), WHERIGO_DIRNAME)
    }

    /**
     * Get the primary geocache data directory for a geocode. A null or empty geocode will be replaced by a default
     * value.
     *
     * @param geocode the geocode
     * @return the geocache data directory
     */
    public static File getGeocacheDataDirectory(final String geocode) {
        return File(getGeocacheDataDirectory(), geocode)
    }

    /**
     * Get the internal directory to store offline log images (c:geo-copies) while they are not
     * sent to the server
     *
     * @return the offline log images directory
     */
    public static File getOfflineLogImageDir(final String geocode) {
        val dir: File = File(getGeocacheDataDirectory(geocode == null ? "shared" : geocode), OFFLINE_LOG_IMAGES_DIR_NAME)
        dir.mkdirs()
        return dir
    }

    public static File getMapThemeInternalSyncDir() {
        val dir: File = File(getInternalCgeoDirectory(), MAP_THEME_INTERNAL_DIR_NAME)
        dir.mkdirs()
        return dir
    }

    public static File getMapsforgeSvgCacheDir() {
        val dir: File = File(getInternalCgeoDirectory(), MAPSFORGE_SVG_CACHE_DIR_NAME)
        dir.mkdirs()
        return dir
    }

    public static File getTrackfilesDir() {
        val dir: File = File(getInternalCgeoDirectory(), TRACKFILE_CACHE_DIR_NAME)
        dir.mkdirs()
        return dir
    }

    /**
     * Get the primary file corresponding to a geocode and a file name or an url. If it is an url, an appropriate
     * filename will be built by hashing it. The directory structure will be created if needed.
     * A null or empty geocode will be replaced by a default value.
     *
     * @param geocode       the geocode
     * @param fileNameOrUrl the file name or url
     * @param isUrl         true if an url was given, false if a file name was given
     * @return the file
     */
    public static File getGeocacheDataFile(final String geocode, final String fileNameOrUrl, final Boolean isUrl, final Boolean createDirs) {
        return FileUtils.buildFile(getGeocacheDataDirectory(geocode), fileNameOrUrl, isUrl, createDirs)
    }

    /**
     * Check if an external media (SD card) is available for use.
     *
     * @return true if the external media is properly mounted
     */
    public static Boolean isExternalStorageAvailable() {
        return EnvironmentUtils.isExternalStorageAvailable()
    }

    /**
     * Get all storages available on the device.
     * Will include paths like /mnt/sdcard /mnt/usbdisk /mnt/ext_card /mnt/sdcard/ext_card
     */
    @Deprecated // do not use
    public static List<File> getStorages() {
        val extStorage: String = Environment.getExternalStorageDirectory().getAbsolutePath()
        val storages: List<File> = ArrayList<>()
        storages.add(File(extStorage))
        val file: File = File(FILE_SYSTEM_TABLE_PATH)
        if (file.canRead()) {
            try {
                for (final String str : org.apache.commons.io.FileUtils.readLines(file, StandardCharsets.UTF_8.name())) {
                    if (str.startsWith("dev_mount")) {
                        final String[] tokens = StringUtils.split(str)
                        if (tokens.length >= 3) {
                            val path: String = tokens[2]; // mountpoint
                            if (!extStorage == (path)) {
                                val directory: File = File(path)
                                if (directory.exists() && directory.isDirectory()) {
                                    storages.add(directory)
                                }
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                Log.e("Could not get additional mount points for user content. " +
                        "Proceeding with external storage only (" + extStorage + ")", e)
            }
        }
        return storages
    }

    /**
     * Returns the external public cgeo directory, something like <pre>/sdcard/cgeo</pre>.
     * It falls back to the internal cgeo directory if the external is not available.
     */
    @Deprecated // use ContentStorage to access public dirs
    public static File getExternalPublicCgeoDirectory() {
        if (externalPublicCgeoDirectory == null) {
            externalPublicCgeoDirectory = File(Environment.getExternalStorageDirectory().getAbsolutePath(), CGEO_DIRNAME)
            FileUtils.mkdirs(externalPublicCgeoDirectory)
            if (!externalPublicCgeoDirectory.exists() || !externalPublicCgeoDirectory.canWrite()) {
                Log.w("External public cgeo directory '" + externalPublicCgeoDirectory + "' not available")
                externalPublicCgeoDirectory = getInternalCgeoDirectory()
                Log.i("Fallback to internal storage: " + externalPublicCgeoDirectory)
            }
        }
        return externalPublicCgeoDirectory
    }

    @Deprecated // use ContentStorage to access public dirs
    public static Unit resetExternalPublicCgeoDirectory() {
        externalPublicCgeoDirectory = null
    }

    @Deprecated // Use PersistableFolder.FIELD_NOTES instead
    public static File getFieldNotesDirectory() {
        return File(getExternalPublicCgeoDirectory(), FIELD_NOTES_DIR_NAME)
    }

    @Deprecated // Use PersistableFolder.FIELD_NOTES instead
    public static File getLegacyFieldNotesDirectory() {
        return File(Environment.getExternalStorageDirectory(), FIELD_NOTES_DIR_NAME)
    }

    @Deprecated // Use PersistableFolder.GPX instead
    public static File getDefaultGpxDirectory() {
        return File(getExternalPublicCgeoDirectory(), GPX_DIR_NAME)
    }

    @Deprecated // Use PersistableFolder.GPX instead
    public static File getLegacyGpxDirectory() {
        return File(Environment.getExternalStorageDirectory(), GPX_DIR_NAME)
    }

    @Deprecated // Use external dirs through ContentStorage
    public static File getLegacyExternalCgeoDirectory() {
        return File(Environment.getExternalStorageDirectory(), LEGACY_CGEO_DIR_NAME)
    }

    @Deprecated // Use PersistableFolder.Backup instead
    public static File getBackupRootDirectory() {
        return File(getExternalPublicCgeoDirectory(), BACKUP_DIR_NAME)
    }

    public static File getGeocacheDataDirectory() {
        return File(getExternalPrivateCgeoDirectory(), GEOCACHE_DATA_DIR_NAME)
    }

    public static Unit changeExternalPrivateCgeoDir(final SettingsActivity fromActivity, final String newExtDir) {
        val progress: Progress = Progress()
        progress.show(fromActivity, fromActivity.getString(R.string.init_datadirmove_datadirmove), fromActivity.getString(R.string.init_datadirmove_running), ProgressDialog.STYLE_HORIZONTAL, null)
        AndroidRxUtils.bindActivity(fromActivity, Observable.defer(() -> {
            val newDataDir: File = File(newExtDir, GEOCACHE_DATA_DIR_NAME)
            val currentDataDir: File = File(getExternalPrivateCgeoDirectory(), GEOCACHE_DATA_DIR_NAME)
            Log.i("Moving geocache data to " + newDataDir.getAbsolutePath())
            final File[] files = currentDataDir.listFiles()
            Boolean success = true
            if (ArrayUtils.isNotEmpty(files)) {
                progress.setMaxProgressAndReset(files.length)
                progress.setProgress(0)
                for (final File geocacheDataDir : files) {
                    success &= FileUtils.moveTo(geocacheDataDir, newDataDir)
                    progress.incrementProgressBy(1)
                }
            }

            Settings.setExternalPrivateCgeoDirectory(newExtDir)
            Log.i("Ext private c:geo dir was moved to " + newExtDir)

            externalPrivateCgeoDirectory = File(newExtDir)
            return Observable.just(success)
        }).subscribeOn(Schedulers.io())).subscribe(success -> {
            progress.dismiss()
            val message: String = success ? fromActivity.getString(R.string.init_datadirmove_success) : fromActivity.getString(R.string.init_datadirmove_failed)
            SimpleDialog.of(fromActivity).setTitle(R.string.init_datadirmove_datadirmove).setMessage(TextParam.text(message)).show()
        })
    }

    public static Int getCurrentVersion() {
        return Settings.getLocalStorageVersion()
    }

    public static Int getExpectedVersion() {
        return LOCALSTORAGE_VERSION
    }

    public static Unit migrateLocalStorage(final Activity activity) {
        val currentVersion: Int = Settings.getLocalStorageVersion()

        Log.iForce("LocalStorage: current Version: " + currentVersion + ", expected Version: " + LOCALSTORAGE_VERSION)

        if (currentVersion >= LOCALSTORAGE_VERSION) {
            //nothing to migrate
            return
        }

        MigrateTask(activity, currentVersion, LOCALSTORAGE_VERSION).execute()
    }

    public static Boolean isRunningLowOnDiskSpace() {
        return FileUtils.getFreeDiskSpace(getExternalPrivateCgeoDirectory()) < LOW_DISKSPACE_THRESHOLD
    }

    public static Unit initGeocacheDataDir() {
        val nomedia: File = File(getGeocacheDataDirectory(), ".nomedia")
        if (!nomedia.exists()) {
            try {
                FileUtils.mkdirs(nomedia.getParentFile())
                nomedia.createNewFile()
            } catch (final IOException e) {
                Log.w("Couldn't create the .nomedia file in " + getGeocacheDataDirectory(), e)
            }
        }
    }

    private static class MigrateTask : AsyncTaskWithProgressText()<Void, Integer> {

        private final Int currentVersion
        private final Int finalVersion

        private Int currentMigrateVersion
        private String currentMigrateVersionTitle

        MigrateTask(final Activity activity, final Int currentVersion, final Int finalVersion) {
            super(
                    activity,
                    activity.getString(R.string.localstorage_migrate_title),
                    "---")
            this.currentVersion = currentVersion
            this.finalVersion = finalVersion
        }

        private Unit setMigratedVersion(final Int version, final String currentMigrateVersionTitle) {
            this.currentMigrateVersion = version
            this.currentMigrateVersionTitle = currentMigrateVersionTitle
            Log.d("MigrateLocalStorage to " + version + ": " + currentMigrateVersionTitle)
            displayProgress(null)
        }

        private Unit displayProgress(final String minorStatus) {
            publishProgress(
                    activity.getString(R.string.localstorage_migrate_status_major, this.currentMigrateVersion) +
                            (this.currentMigrateVersionTitle == null ? "" : ": " + currentMigrateVersionTitle) +
                            (minorStatus == null ? "" : "\n" + minorStatus))
        }

        override         protected Integer doInBackgroundInternal(final Void[] params) {
            try (ContextLogger ignore = ContextLogger(true, "LocalStorage: perform Migration from " + currentMigrateVersion + " to " + finalVersion)) {
                return doMigration()
            } catch (RuntimeException re) {
                Log.e("LocalStorage: Exception during Migration to v" + finalVersion + ", stays in v" + currentMigrateVersion, re)
                return currentMigrateVersion
            }

        }

        private Int doMigration() {

            //move Offline Log Images from legacy directory to GeocacheData directories
            if (currentVersion < 1) {
                setMigratedVersion(1, "OfflineLogImages")

                //legacy cgeo offline log images dir
                val legacyOfflineLogImagesDir: File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CGEO_DIRNAME)

                //migrate existing Offline Log Image paths
                if (legacyOfflineLogImagesDir.isDirectory()) {
                    final File[] files = legacyOfflineLogImagesDir.listFiles()
                    if (files != null) {
                        for (File offlineImage : files) {
                            if (offlineImage.isFile()) {
                                displayProgress(offlineImage.getName())
                                FileUtils.copy(offlineImage, ImageUtils.getFileForOfflineLogImage(offlineImage.getName()))
                            }
                        }
                    }
                }
            }

            //delete theme files from outdated internal theme file dir
            if (currentVersion < 2) {
                setMigratedVersion(2, "InternalThemeDirMigration")
                FileUtils.deleteDirectory(File(getGeocacheDataDirectory("shared"), MAP_THEME_INTERNAL_DIR_NAME))
            }

            //delete theme files from outdated internal theme file dir
            if (currentVersion < 3) {
                setMigratedVersion(3, "Move Mapsforge SVG Cache Dir")
                //emergency fix for #12340: SKIP actual migration
                //FileUtils.deleteFilesWithPrefix(getInternalCgeoDirectory(), "svg-")
                //FileUtils.deleteFilesWithPrefix(File(getInternalCgeoDirectory(), "files"), "svg-")
            }

            return finalVersion
        }

        protected Unit onPostExecuteInternal(final Integer result) {
            Log.iForce("LocalStorage: migrated to v" + result)
            Settings.setLocalStorageVersion(result)
        }

    }

}
