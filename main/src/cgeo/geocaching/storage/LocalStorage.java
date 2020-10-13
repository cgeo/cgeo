package cgeo.geocaching.storage;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.Progress;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.SettingsActivity;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.EnvironmentUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private static final String CGEO_DIRNAME = "cgeo";
    private static final String DATABASES_DIRNAME = "databases";
    private static final String BACKUP_DIR_NAME = "backup";
    public static final String LOGFILES_DIR_NAME = "logfiles";
    private static final String MAP_DIR_NAME = "maps";
    private static final String GPX_DIR_NAME = "gpx";
    private static final String FIELD_NOTES_DIR_NAME = "field-notes";
    private static final String LEGACY_CGEO_DIR_NAME = ".cgeo";
    private static final String GEOCACHE_PHOTOS_DIR_NAME = "GeocachePhotos";
    private static final String GEOCACHE_DATA_DIR_NAME = "GeocacheData";
    private static final long LOW_DISKSPACE_THRESHOLD = 1024 * 1024 * 100; // 100 MB in bytes

    private static File internalCgeoDirectory;
    private static File externalPrivateCgeoDirectory;
    private static File externalPublicCgeoDirectory;

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
     * @param geocode
     *            the geocode
     * @return the geocache data directory
     */
    @NonNull
    public static File getGeocacheDataDirectory(@NonNull final String geocode) {
        return new File(getGeocacheDataDirectory(), geocode);
    }

    /**
     * Get the primary file corresponding to a geocode and a file name or an url. If it is an url, an appropriate
     * filename will be built by hashing it. The directory structure will be created if needed.
     * A null or empty geocode will be replaced by a default value.
     *
     * @param geocode
     *            the geocode
     * @param fileNameOrUrl
     *            the file name or url
     * @param isUrl
     *            true if an url was given, false if a file name was given
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
     * @param geocode
     *            The geocode identifying the cache directory
     * @param prefix
     *            The filename prefix
     */
    public static void deleteCacheFilesWithPrefix(@NonNull final String geocode, @NonNull final String prefix) {
        FileUtils.deleteFilesWithPrefix(getGeocacheDataDirectory(geocode), prefix);
    }

    /**
     * Get all storages available on the device.
     * Will include paths like /mnt/sdcard /mnt/usbdisk /mnt/ext_card /mnt/sdcard/ext_card
     */
    @NonNull
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
    public static File getExternalPublicCgeoDirectory() {
        if (externalPublicCgeoDirectory == null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                final Uri baseDir = Settings.getBaseDir();
                externalPublicCgeoDirectory = null != baseDir && null != baseDir.getPath() ? new File(baseDir.getPath()) : new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), CGEO_DIRNAME);
            } else {
                externalPublicCgeoDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), CGEO_DIRNAME);
            }
            FileUtils.mkdirs(externalPublicCgeoDirectory);
            if (!externalPublicCgeoDirectory.exists() || !externalPublicCgeoDirectory.canWrite()) {
                Log.w("External public cgeo directory '" + externalPublicCgeoDirectory + "' not available");
                externalPublicCgeoDirectory = getInternalCgeoDirectory();
                Log.i("Fallback to internal storage: " + externalPublicCgeoDirectory);
            }
        }
        return externalPublicCgeoDirectory;
    }

    public static void resetExternalPublicCgeoDirectory() {
        externalPublicCgeoDirectory = null;
    }

    @NonNull
    public static File getFieldNotesDirectory() {
        return new File(getExternalPublicCgeoDirectory(), FIELD_NOTES_DIR_NAME);
    }

    @NonNull
    public static File getLegacyFieldNotesDirectory() {
        return new File(Environment.getExternalStorageDirectory(), FIELD_NOTES_DIR_NAME);
    }

    @NonNull
    public static File getDefaultMapDirectory() {
        return new File(getExternalPublicCgeoDirectory(), MAP_DIR_NAME);
    }

    @NonNull
    public static File getDefaultGpxDirectory() {
        return new File(getExternalPublicCgeoDirectory(), GPX_DIR_NAME);
    }

    @NonNull
    public static File getGpxExportDirectory() {
        final File gpxExportDir =  new File(Settings.getGpxExportDir());
        FileUtils.mkdirs(gpxExportDir);
        if (!gpxExportDir.isDirectory() || !gpxExportDir.canWrite()) {
            return getDefaultGpxDirectory();
        }
        return gpxExportDir;
    }

    @NonNull
    public static File getGpxImportDirectory() {
        return new File(Settings.getGpxImportDir());
    }

    @NonNull
    public static File getLegacyGpxDirectory() {
        return new File(Environment.getExternalStorageDirectory(), GPX_DIR_NAME);
    }

    @NonNull
    public static File getLegacyExternalCgeoDirectory() {
        return new File(Environment.getExternalStorageDirectory(), LEGACY_CGEO_DIR_NAME);
    }

    @Nullable
    public static File getNewBackupDirectory(final long timestamp) {
        final File newFolder = new File(getBackupRootDirectory(), Formatter.formatDateForFilename(timestamp));

        if (newFolder.exists()) {
            return null; // We don't want to overwrite a existing backup
        }

        FileUtils.mkdirs(newFolder);
        return newFolder;
    }

    @NonNull
    public static File getBackupRootDirectory() {
        return new File(getExternalPublicCgeoDirectory(), BACKUP_DIR_NAME);
    }

    @NonNull
    public static File getLogfilesDirectory() {
        return new File(getExternalPublicCgeoDirectory(), LOGFILES_DIR_NAME);
    }

    @NonNull
    public static File getGeocacheDataDirectory() {
        return new File(getExternalPrivateCgeoDirectory(), GEOCACHE_DATA_DIR_NAME);
    }

    @NonNull
    public static File getLocalSpoilersDirectory() {
        return new File(getExternalPublicCgeoDirectory(), GEOCACHE_PHOTOS_DIR_NAME);
    }

    @NonNull
    public static File getLegacyLocalSpoilersDirectory() {
        return new File(Environment.getExternalStorageDirectory(), GEOCACHE_PHOTOS_DIR_NAME);
    }

    @NonNull
    public static List<File> getMapDirectories() {
        final List<File> folders = new ArrayList<>();
        for (final File dir : getStorages()) {
            folders.add(new File(dir, "mfmaps"));
            folders.add(new File(new File(dir, "Locus"), "mapsVector"));
            folders.add(new File(dir, CGEO_DIRNAME));
        }
        return folders;
    }

    @NonNull
    public static File getLogPictureDirectory() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CGEO_DIRNAME);
    }

    public static void deleteFilesOrDirectories(final File[] files) {
        if (files == null) {
            return;
        }
        for (File file : files) {
            deleteRecursive(file);
        }
    }

    private static void deleteRecursive(final File fileOrDirectory) {
        try {
            if (fileOrDirectory.isDirectory()) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        } catch (Exception e) {
            Log.w("Couldn't delete " + fileOrDirectory, e);
        }

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
            Dialogs.message(fromActivity, R.string.init_datadirmove_datadirmove, message);
        });
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
}
