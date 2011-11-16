package cgeo.geocaching.files;

import cgeo.geocaching.Settings;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handle local storage issues on phone and SD card.
 *
 */
public class LocalStorage {

    /** Name of the local private directory to use to hold cached information */
    public final static String cache = ".cgeo";

    /**
     * Return the primary storage cache root (external media if mounted, phone otherwise).
     *
     * @return the root of the cache directory
     */
    public static File getStorage() {
        return getStorageSpecific(false);
    }

    /**
     * Return the secondary storage cache root (phone if external media is mounted, external media otherwise).
     *
     * @return the root of the cache directory
     */
    public static File getStorageSec() {
        return getStorageSpecific(true);
    }

    private static File getStorageSpecific(boolean secondary) {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ^ secondary ?
                new File(Environment.getExternalStorageDirectory(), LocalStorage.cache) :
                new File(new File(new File(Environment.getDataDirectory(), "data"), "cgeo.geocaching"), LocalStorage.cache);
    }

    /**
     * Get the guessed file extension of an URL. A file extension can contain up-to 4 characters in addition to the dot.
     *
     * @param url
     *            the relative or absolute URL
     * @return the file extension, including the leading dot, or the empty string if none could be determined
     */
    static String getExtension(final String url) {
        final String urlExt = StringUtils.substringAfterLast(url, ".");
        if (urlExt.length() > 4) {
            return "";
        } else if (urlExt.length() > 0) {
            return "." + urlExt;
        }
        return "";
    }

    /**
     * Get the primary storage cache directory for a geocode. A null or empty geocode will be replaced by a default
     * value.
     *
     * @param geocode
     *            the geocode
     * @return the cache directory
     */
    public static File getStorageDir(final String geocode) {
        return storageDir(getStorage(), geocode);
    }

    /**
     * Get the secondary storage cache directory for a geocode. A null or empty geocode will be replaced by a default
     * value.
     *
     * @param geocode
     *            the geocode
     * @return the cache directory
     */
    private static File getStorageSecDir(final String geocode) {
        return storageDir(getStorageSec(), geocode);
    }

    private static File storageDir(final File base, final String geocode) {
        return new File(base, StringUtils.defaultIfEmpty(geocode, "_others"));
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
    public static File getStorageFile(final String geocode, final String fileNameOrUrl, final boolean isUrl, final boolean createDirs) {
        return buildFile(getStorageDir(geocode), fileNameOrUrl, isUrl, createDirs);
    }

    /**
     * Get the secondary file corresponding to a geocode and a file name or an url. If it is an url, an appropriate
     * filename will be built by hashing it. The directory structure will not be created automatically.
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
    public static File getStorageSecFile(final String geocode, final String fileNameOrUrl, final boolean isUrl) {
        return buildFile(getStorageSecDir(geocode), fileNameOrUrl, isUrl, false);
    }

    private static File buildFile(final File base, final String fileName, final boolean isUrl, final boolean createDirs) {
        if (createDirs) {
            base.mkdirs();
        }
        return new File(base, isUrl ? CryptUtils.md5(fileName) + getExtension(fileName) : fileName);
    }

    /**
     * Save an HTTP response to a file.
     *
     * @param entity
     *            the entity whose content will be saved
     * @param targetFile
     *            the target file, which will be created if necessary
     * @return true if the operation was sucessful, false otherwise
     */
    public static boolean saveEntityToFile(final HttpEntity entity, final File targetFile) {
        if (entity == null) {
            return false;
        }

        try {
            final InputStream is = entity.getContent();
            try {
                final FileOutputStream fos = new FileOutputStream(targetFile);
                try {
                    return copy(is, fos);
                } finally {
                    fos.close();
                }
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.e(Settings.tag, "LocalStorage.saveEntityToFile", e);
        }
        return false;
    }

    /**
     * Copy a file into another. The directory structure of target file will be created if needed.
     *
     * @param source
     *            the source file
     * @param destination
     *            the target file
     * @return true if the copy happened without error, false otherwise
     */
    public static boolean copy(final File source, final File destination) {
        destination.getParentFile().mkdirs();

        InputStream input;
        OutputStream output;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);
        } catch (FileNotFoundException e) {
            Log.e(Settings.tag, "LocalStorage.copy: could not open file", e);
            return false;
        }

        boolean copyDone = copy(input, output);

        try {
            input.close();
            output.close();
        } catch (IOException e) {
            Log.e(Settings.tag, "LocalStorage.copy: could not close file", e);
            return false;
        }

        return copyDone;
    }

    private static boolean copy(final InputStream input, final OutputStream output) {
        byte[] buffer = new byte[4096];
        int length;
        try {
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            output.flush(); // FIXME: is that necessary?
        } catch (IOException e) {
            Log.e(Settings.tag, "LocalStorage.copy: error when copying data", e);
            return false;
        }

        return true;
    }

    /**
     * Check if an external media (SD card) is available for use.
     *
     * @return true if the external media is properly mounted
     */
    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

}
