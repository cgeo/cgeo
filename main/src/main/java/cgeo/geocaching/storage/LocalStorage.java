package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.AsyncTaskWithProgressText;
import cgeo.geocaching.utils.ContextLogger;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.ImageUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Handle local storage issues on phone and SD card.
 */
public final class LocalStorage {

    public static final Pattern GEOCACHE_FILE_PATTERN = Pattern.compile("^(GC|TB|TC|CC|LC|EC|GK|MV|TR|VI|MS|EV|CT|GE|GA|WM|O)[A-Z0-9]{2,7}$");

    private static final String FILE_SYSTEM_TABLE_PATH = "/system/etc/vold.fstab";
    private static final String DATABASES_DIRNAME = "databases";
    private static final String GEOCACHE_DATA_DIR_NAME = "GeocacheData";
    private static final String OFFLINE_LOG_IMAGES_DIR_NAME = "OfflineLogImages";
    private static final String MAP_THEME_INTERNAL_DIR_NAME = "MapThemeData";
    private static final String MAPSFORGE_SVG_CACHE_DIR_NAME = "mapsforge-svg-cache";
    private static final String TRACKFILE_CACHE_DIR_NAME = "trackfiles";
    private static final long LOW_DISKSPACE_THRESHOLD = 1024 * 1024 * 100; // 100 MB in bytes

    //Legacy directory names which should NO LONGER BE OF USE
    private static final String CGEO_DIRNAME = "cgeo"; //legacy
    private static final String BACKUP_DIR_NAME = "backup"; //legacy
    private static final String GPX_DIR_NAME = "gpx"; //legacy
    private static final String FIELD_NOTES_DIR_NAME = "field-notes"; //legacy
    private static final String LEGACY_CGEO_DIR_NAME = ".cgeo";  // double legacy


    private static File internalCgeoDirectory;
    private static File externalPrivateCgeoDirectory;
    private static File externalPublicCgeoDirectory;

    private static final int LOCALSTORAGE_VERSION = 3;


    private LocalStorage() {
        // utility class
    }

    /**
     * Usually <pre>/data/data/cgeo.geocaching</pre>
     */
    @NonNull
    public static File getInternalCgeoDirectory() {
        if (internalCgeoDirectory == null) {
            // A race condition will do no harm as the operation is idempotent. No need to synchronize.
            internalCgeoDirectory = CgeoApplication.getInstance().getApplicationContext().getFilesDir().getParentFile();
        }
        return internalCgeoDirectory;
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
    @NonNull
    public static List<File> getAvailableExternalPrivateCgeoDirectories() {
        final List<File> availableExtDirs = new ArrayList<>();
        final File[] externalFilesDirs = ContextCompat.getExternalFilesDirs(CgeoApplication.getInstance(), null);
        for (final File dir : externalFilesDirs) {
            if (dir != null && EnvironmentCompat.getStorageState(dir).equals(Environment.MEDIA_MOUNTED)) {
                availableExtDirs.add(dir);
                Log.i("Added '" + dir + "' as available external dir");
            } else {
                Log.w("'" + dir + "' is NOT available as external dir");
            }
        }
        return availableExtDirs;
    }

    /**
     * Usually one of {@link LocalStorage#getAvailableExternalPrivateCgeoDirectories()}.
     * Fallback to {@link LocalStorage#getFirstExternalPrivateCgeoDirectory()}
     */
    @NonNull
    public static File getExternalPrivateCgeoDirectory() {
        if (externalPrivateCgeoDirectory == null) {
            // find the one selected in preferences
            final String prefDirectory = Settings.getExternalPrivateCgeoDirectory();
            for (final File dir : getAvailableExternalPrivateCgeoDirectories()) {
                if (dir.getAbsolutePath().equals(prefDirectory)) {
                    externalPrivateCgeoDirectory = dir;
                    break;
                }
            }

            // fallback to default external files dir
            if (externalPrivateCgeoDirectory == null) {
                Log.w("Chosen extCgeoDir " + prefDirectory + " is not an available external dir, falling back to default extCgeoDir");
                externalPrivateCgeoDirectory = getFirstExternalPrivateCgeoDirectory();
            }

            if (prefDirectory == null) {
                Settings.setExternalPrivateCgeoDirectory(externalPrivateCgeoDirectory.getAbsolutePath());
            }
        }
        return externalPrivateCgeoDirectory;
    }

    /**
     * Uses {@link android.content.Context#getExternalFilesDir(String)} with "null".
     * This is usually the emulated external storage.
     * It falls back to {@link LocalStorage#getInternalCgeoDirectory()}.
     */
    @NonNull
    public static File getFirstExternalPrivateCgeoDirectory() {
        final File externalFilesDir = CgeoApplication.getInstance().getExternalFilesDir(null);

        // fallback to internal dir
        if (externalFilesDir == null) {
            Log.w("No extCgeoDir is available, falling back to internal storage");
            return getInternalCgeoDirectory();
        }

        return externalFilesDir;
    }

    @NonNull
    public static File getExternalDbDirectory() {
        return new File(getFirstExternalPrivateCgeoDirectory(), DATABASES_DIRNAME);
    }

    @NonNull
    public static File getInternalDbDirectory() {
        return new File(getInternalCgeoDirectory(), DATABASES_DIRNAME);
    }

    /**
     * Get the primary geocache data directory for a geocode. A null or empty geocode will be replaced by a default
     * value.
     *
     * @param geocode the geocode
     * @return the geocache data directory
     */
    @NonNull
    public static File getGeocacheDataDirectory(@NonNull final String geocode) {
        return new File(getGeocacheDataDirectory(), geocode);
    }

    /**
     * Get the internal directory to store offline log images (c:geo-copies) while they are not
     * sent to the server
     *
     * @return the offline log images directory
     */
    @NonNull
    public static File getOfflineLogImageDir(final String geocode) {
        final File dir = new File(getGeocacheDataDirectory(geocode == null ? "shared" : geocode), OFFLINE_LOG_IMAGES_DIR_NAME);
        dir.mkdirs();
        return dir;
    }

    @NonNull
    public static File getMapThemeInternalSyncDir() {
        final File dir = new File(getInternalCgeoDirectory(), MAP_THEME_INTERNAL_DIR_NAME);
        dir.mkdirs();
        return dir;
    }

    @NonNull
    public static File getMapsforgeSvgCacheDir() {
        final File dir = new File(getInternalCgeoDirectory(), MAPSFORGE_SVG_CACHE_DIR_NAME);
        dir.mkdirs();
        return dir;
    }

    @NonNull
    public static File getTrackfilesDir() {
        final File dir = new File(getInternalCgeoDirectory(), TRACKFILE_CACHE_DIR_NAME);
        dir.mkdirs();
        return dir;
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
    @NonNull
    public static File getGeocacheDataFile(@NonNull final String geocode, @NonNull final String fileNameOrUrl, final boolean isUrl, final boolean createDirs) {
        return FileUtils.buildFile(getGeocacheDataDirectory(geocode), fileNameOrUrl, isUrl, createDirs);
    }

    /**
     * Check if an external media (SD card) is available for use.
     *
     * @return true if the external media is properly mounted
     */
    public static boolean isExternalStorageAvailable() {
        return EnvironmentUtils.isExternalStorageAvailable();
    }

    /**
     * Deletes all files from geocode cache directory with the given prefix.
     *
     * @param geocode The geocode identifying the cache directory
     * @param prefix  The filename prefix
     */
    public static void deleteCacheFilesWithPrefix(@NonNull final String geocode, @NonNull final String prefix) {
        FileUtils.deleteFilesWithPrefix(getGeocacheDataDirectory(geocode), prefix);
    }

    /**
     * Get all storages available on the device.
     * Will include paths like /mnt/sdcard /mnt/usbdisk /mnt/ext_card /mnt/sdcard/ext_card
     */
    @NonNull
    @Deprecated // do not use
    public static List<File> getStorages() {
        final String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        final List<File> storages = new ArrayList<>();
        storages.add(new File(extStorage));
        final File file = new File(FILE_SYSTEM_TABLE_PATH);
        if (file.canRead()) {
            try {
                for (final String str : org.apache.commons.io.FileUtils.readLines(file, StandardCharsets.UTF_8.name())) {
                    if (str.startsWith("dev_mount")) {
                        final String[] tokens = StringUtils.split(str);
                        if (tokens.length >= 3) {
                            final String path = tokens[2]; // mountpoint
                            if (!extStorage.equals(path)) {
                                final File directory = new File(path);
                                if (directory.exists() && directory.isDirectory()) {
                                    storages.add(directory);
                                }
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                Log.e("Could not get additional mount points for user content. " +
                        "Proceeding with external storage only (" + extStorage + ")", e);
            }
        }
        return storages;
    }

    /**
     * Returns the external public cgeo directory, something like <pre>/sdcard/cgeo</pre>.
     * It falls back to the internal cgeo directory if the external is not available.
     */
    @NonNull
    @Deprecated // use ContentStorage to access public dirs
    public static File getExternalPublicCgeoDirectory() {
        if (externalPublicCgeoDirectory == null) {
            externalPublicCgeoDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), CGEO_DIRNAME);
            FileUtils.mkdirs(externalPublicCgeoDirectory);
            if (!externalPublicCgeoDirectory.exists() || !externalPublicCgeoDirectory.canWrite()) {
                Log.w("External public cgeo directory '" + externalPublicCgeoDirectory + "' not available");
                externalPublicCgeoDirectory = getInternalCgeoDirectory();
                Log.i("Fallback to internal storage: " + externalPublicCgeoDirectory);
            }
        }
        return externalPublicCgeoDirectory;
    }

    @Deprecated // use ContentStorage to access public dirs
    public static void resetExternalPublicCgeoDirectory() {
        externalPublicCgeoDirectory = null;
    }

    @NonNull
    @Deprecated // Use PersistableFolder.FIELD_NOTES instead
    public static File getFieldNotesDirectory() {
        return new File(getExternalPublicCgeoDirectory(), FIELD_NOTES_DIR_NAME);
    }

    @NonNull
    @Deprecated // Use PersistableFolder.FIELD_NOTES instead
    public static File getLegacyFieldNotesDirectory() {
        return new File(Environment.getExternalStorageDirectory(), FIELD_NOTES_DIR_NAME);
    }

    @NonNull
    @Deprecated // Use PersistableFolder.GPX instead
    public static File getDefaultGpxDirectory() {
        return new File(getExternalPublicCgeoDirectory(), GPX_DIR_NAME);
    }

    @NonNull
    @Deprecated // Use PersistableFolder.GPX instead
    public static File getLegacyGpxDirectory() {
        return new File(Environment.getExternalStorageDirectory(), GPX_DIR_NAME);
    }

    @NonNull
    @Deprecated // Use external dirs through ContentStorage
    public static File getLegacyExternalCgeoDirectory() {
        return new File(Environment.getExternalStorageDirectory(), LEGACY_CGEO_DIR_NAME);
    }

    @NonNull
    @Deprecated // Use PersistableFolder.Backup instead
    public static File getBackupRootDirectory() {
        return new File(getExternalPublicCgeoDirectory(), BACKUP_DIR_NAME);
    }

    @NonNull
    public static File getGeocacheDataDirectory() {
        return new File(getExternalPrivateCgeoDirectory(), GEOCACHE_DATA_DIR_NAME);
    }

    public static void changeExternalPrivateCgeoDir(final SettingsActivity fromActivity, final String newExtDir) {
        final Progress progress = new Progress();
        progress.show(fromActivity, fromActivity.getString(R.string.init_datadirmove_datadirmove), fromActivity.getString(R.string.init_datadirmove_running), ProgressDialog.STYLE_HORIZONTAL, null);
        AndroidRxUtils.bindActivity(fromActivity, Observable.defer(() -> {
            final File newDataDir = new File(newExtDir, GEOCACHE_DATA_DIR_NAME);
            final File currentDataDir = new File(getExternalPrivateCgeoDirectory(), GEOCACHE_DATA_DIR_NAME);
            Log.i("Moving geocache data to " + newDataDir.getAbsolutePath());
            final File[] files = currentDataDir.listFiles();
            boolean success = true;
            if (ArrayUtils.isNotEmpty(files)) {
                progress.setMaxProgressAndReset(files.length);
                progress.setProgress(0);
                for (final File geocacheDataDir : files) {
                    success &= FileUtils.moveTo(geocacheDataDir, newDataDir);
                    progress.incrementProgressBy(1);
                }
            }

            Settings.setExternalPrivateCgeoDirectory(newExtDir);
            Log.i("Ext private c:geo dir was moved to " + newExtDir);

            externalPrivateCgeoDirectory = new File(newExtDir);
            return Observable.just(success);
        }).subscribeOn(Schedulers.io())).subscribe(success -> {
            progress.dismiss();
            final String message = success ? fromActivity.getString(R.string.init_datadirmove_success) : fromActivity.getString(R.string.init_datadirmove_failed);
            SimpleDialog.of(fromActivity).setTitle(R.string.init_datadirmove_datadirmove).setMessage(TextParam.text(message)).show();
        });
    }

    public static int getCurrentVersion() {
        return Settings.getLocalStorageVersion();
    }

    public static int getExpectedVersion() {
        return LOCALSTORAGE_VERSION;
    }

    public static void migrateLocalStorage(final Activity activity) {
        final int currentVersion = Settings.getLocalStorageVersion();

        Log.iForce("LocalStorage: current Version: " + currentVersion + ", expected Version: " + LOCALSTORAGE_VERSION);

        if (currentVersion >= LOCALSTORAGE_VERSION) {
            //nothing to migrate
            return;
        }

        new MigrateTask(activity, currentVersion, LOCALSTORAGE_VERSION).execute();
    }

    public static boolean isRunningLowOnDiskSpace() {
        return FileUtils.getFreeDiskSpace(getExternalPrivateCgeoDirectory()) < LOW_DISKSPACE_THRESHOLD;
    }

    public static void initGeocacheDataDir() {
        final File nomedia = new File(getGeocacheDataDirectory(), ".nomedia");
        if (!nomedia.exists()) {
            try {
                FileUtils.mkdirs(nomedia.getParentFile());
                nomedia.createNewFile();
            } catch (final IOException e) {
                Log.w("Couldn't create the .nomedia file in " + getGeocacheDataDirectory(), e);
            }
        }
    }

    private static class MigrateTask extends AsyncTaskWithProgressText<Void, Integer> {

        private final int currentVersion;
        private final int finalVersion;

        private int currentMigrateVersion;
        private String currentMigrateVersionTitle;

        MigrateTask(@NonNull final Activity activity, final int currentVersion, final int finalVersion) {
            super(
                    activity,
                    activity.getString(R.string.localstorage_migrate_title),
                    "---");
            this.currentVersion = currentVersion;
            this.finalVersion = finalVersion;
        }

        private void setMigratedVersion(final int version, final String currentMigrateVersionTitle) {
            this.currentMigrateVersion = version;
            this.currentMigrateVersionTitle = currentMigrateVersionTitle;
            Log.d("MigrateLocalStorage to " + version + ": " + currentMigrateVersionTitle);
            displayProgress(null);
        }

        private void displayProgress(final String minorStatus) {
            publishProgress(
                    activity.getString(R.string.localstorage_migrate_status_major, this.currentMigrateVersion) +
                            (this.currentMigrateVersionTitle == null ? "" : ": " + currentMigrateVersionTitle) +
                            (minorStatus == null ? "" : "\n" + minorStatus));
        }

        @Override
        protected Integer doInBackgroundInternal(final Void[] params) {
            try (ContextLogger ignore = new ContextLogger(true, "LocalStorage: perform Migration from " + currentMigrateVersion + " to " + finalVersion)) {
                return doMigration();
            } catch (RuntimeException re) {
                Log.e("LocalStorage: Exception during Migration to v" + finalVersion + ", stays in v" + currentMigrateVersion, re);
                return currentMigrateVersion;
            }

        }

        private int doMigration() {

            //move Offline Log Images from legacy directory to GeocacheData directories
            if (currentVersion < 1) {
                setMigratedVersion(1, "OfflineLogImages");

                //legacy cgeo offline log images dir
                final File legacyOfflineLogImagesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CGEO_DIRNAME);

                //migrate existing Offline Log Image paths
                if (legacyOfflineLogImagesDir.isDirectory()) {
                    final File[] files = legacyOfflineLogImagesDir.listFiles();
                    if (files != null) {
                        for (File offlineImage : files) {
                            if (offlineImage.isFile()) {
                                displayProgress(offlineImage.getName());
                                FileUtils.copy(offlineImage, ImageUtils.getFileForOfflineLogImage(offlineImage.getName()));
                            }
                        }
                    }
                }
            }

            //delete theme files from outdated internal theme file dir
            if (currentVersion < 2) {
                setMigratedVersion(2, "InternalThemeDirMigration");
                FileUtils.deleteDirectory(new File(getGeocacheDataDirectory("shared"), MAP_THEME_INTERNAL_DIR_NAME));
            }

            //delete theme files from outdated internal theme file dir
            if (currentVersion < 3) {
                setMigratedVersion(3, "Move Mapsforge SVG Cache Dir");
                //emergency fix for #12340: SKIP actual migration
                //FileUtils.deleteFilesWithPrefix(getInternalCgeoDirectory(), "svg-");
                //FileUtils.deleteFilesWithPrefix(new File(getInternalCgeoDirectory(), "files"), "svg-");
            }

            return finalVersion;
        }

        protected void onPostExecuteInternal(final Integer result) {
            Log.iForce("LocalStorage: migrated to v" + result);
            Settings.setLocalStorageVersion(result);
        }

    }

}
