package cgeo.geocaching.files;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle local storage issues on phone and SD card.
 *
 */
public final class LocalStorage {

    private static final String FILE_SYSTEM_TABLE_PATH = "/system/etc/vold.fstab";
    public static final String HEADER_LAST_MODIFIED = "last-modified";
    public static final String HEADER_ETAG = "etag";

    /** Name of the local private directory used to hold cached information */
    public final static String cache = ".cgeo";

    private static File internalStorageBase;

    private LocalStorage() {
        // utility class
    }

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

    private static File getStorageSpecific(final boolean secondary) {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ^ secondary ?
                getExternalStorageBase() :
                new File(getInternalStorageBase(), LocalStorage.cache);
    }

    public static File getExternalDbDirectory() {
        return getExternalStorageBase();
    }

    public static File getInternalDbDirectory() {
        return new File(getInternalStorageBase(), "databases");
    }

    private static File getExternalStorageBase() {
        return new File(Environment.getExternalStorageDirectory(), LocalStorage.cache);
    }

    private static File getInternalStorageBase() {
        if (internalStorageBase == null) {
            // A race condition will do no harm as the operation is idempotent. No need to synchronize.
            internalStorageBase = CgeoApplication.getInstance().getApplicationContext().getFilesDir().getParentFile();
        }
        return internalStorageBase;
    }

    /**
     * Get the guessed file extension of an URL. A file extension can contain up-to 4 characters in addition to the dot.
     *
     * @param url
     *            the relative or absolute URL
     * @return the file extension, including the leading dot, or the empty string if none could be determined
     */
    static String getExtension(final String url) {
        String urlExt;
        if (url.startsWith("data:")) {
            // "data:image/png;base64,i53…" -> ".png"
            urlExt = StringUtils.substringAfter(StringUtils.substringBefore(url, ";"), "/");
        } else {
            // "http://example.com/foo/bar.png" -> ".png"
            urlExt = StringUtils.substringAfterLast(url, ".");
        }
        return urlExt.length() >= 1 && urlExt.length() <= 4 ? "." + urlExt : "";
    }

    /**
     * Get the primary storage cache directory for a geocode. A null or empty geocode will be replaced by a default
     * value.
     *
     * @param geocode
     *            the geocode
     * @return the cache directory
     */
    public static File getStorageDir(@Nullable final String geocode) {
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
    private static File getStorageSecDir(@Nullable final String geocode) {
        return storageDir(getStorageSec(), geocode);
    }

    private static File storageDir(final File base, @Nullable final String geocode) {
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
    public static File getStorageFile(@Nullable final String geocode, final String fileNameOrUrl, final boolean isUrl, final boolean createDirs) {
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
            FileUtils.mkdirs(base);
        }
        return new File(base, isUrl ? CryptUtils.md5(fileName) + getExtension(fileName) : fileName);
    }

    /**
     * Save an HTTP response to a file.
     *
     * @param response
     *            the response whose entity content will be saved
     * @param targetFile
     *            the target file, which will be created if necessary
     * @return true if the operation was successful, false otherwise, in which case the file will not exist
     */
    public static boolean saveEntityToFile(final HttpResponse response, final File targetFile) {
        if (response == null) {
            return false;
        }

        try {
            final boolean saved = saveToFile(response.getEntity().getContent(), targetFile);
            saveHeader(HEADER_ETAG, saved ? response : null, targetFile);
            saveHeader(HEADER_LAST_MODIFIED, saved ? response : null, targetFile);
            return saved;
        } catch (final IOException e) {
            Log.e("LocalStorage.saveEntityToFile", e);
        }

        return false;
    }

    private static void saveHeader(final String name, @Nullable final HttpResponse response, final File baseFile) {
        final Header header = response != null ? response.getFirstHeader(name) : null;
        final File file = filenameForHeader(baseFile, name);
        if (header == null) {
            FileUtils.deleteIgnoringFailure(file);
        } else {
            try {
                saveToFile(new ByteArrayInputStream(header.getValue().getBytes("UTF-8")), file);
            } catch (final UnsupportedEncodingException e) {
                // Do not try to display the header in the log message, as our default encoding is
                // likely to be UTF-8 and it will fail as well.
                Log.e("LocalStorage.saveHeader: unable to decode header", e);
            }
        }
    }

    private static File filenameForHeader(final File baseFile, final String name) {
        return new File(baseFile.getAbsolutePath() + "-" + name);
    }

    /**
     * Get the saved header value for this file.
     *
     * @param baseFile
     *            the name of the cached resource
     * @param name
     *            the name of the header ("etag" or "last-modified")
     * @return the cached value, or <tt>null</tt> if none has been cached
     */
    @Nullable
    public static String getSavedHeader(final File baseFile, final String name) {
        try {
            final File file = filenameForHeader(baseFile, name);
            final Reader f = new InputStreamReader(new FileInputStream(file), CharEncoding.UTF_8);
            try {
                // No header will be more than 256 bytes
                final char[] value = new char[256];
                final int count = f.read(value);
                return new String(value, 0, count);
            } finally {
                f.close();
            }
        } catch (final FileNotFoundException ignored) {
            // Do nothing, the file does not exist
        } catch (final Exception e) {
            Log.w("could not read saved header " + name + " for " + baseFile, e);
        }
        return null;
    }

    /**
     * Save a stream to a file.
     * <p/>
     * If the response could not be saved to the file due, for example, to a network error, the file will not exist when
     * this method returns.
     *
     * @param inputStream
     *            the stream whose content will be saved
     * @param targetFile
     *            the target file, which will be created if necessary
     * @return true if the operation was successful, false otherwise
     */
    public static boolean saveToFile(final InputStream inputStream, final File targetFile) {
        if (inputStream == null) {
            return false;
        }


        try {
            try {
                final File tempFile = File.createTempFile("download", null, targetFile.getParentFile());
                final FileOutputStream fos = new FileOutputStream(tempFile);
                final boolean written = copy(inputStream, fos);
                fos.close();
                if (written) {
                    return tempFile.renameTo(targetFile);
                }
                FileUtils.deleteIgnoringFailure(tempFile);
                return false;
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        } catch (final IOException e) {
            Log.e("LocalStorage.saveToFile", e);
            FileUtils.deleteIgnoringFailure(targetFile);
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
        FileUtils.mkdirs(destination.getParentFile());

        InputStream input = null;
        OutputStream output = null;
        boolean copyDone = false;

        try {
            input = new BufferedInputStream(new FileInputStream(source));
            output = new BufferedOutputStream(new FileOutputStream(destination));
            copyDone = copy(input, output);
            // close here already to catch any issue with closing
            input.close();
            output.close();
        } catch (final FileNotFoundException e) {
            Log.e("LocalStorage.copy: could not copy file", e);
            return false;
        } catch (final IOException e) {
            Log.e("LocalStorage.copy: could not copy file", e);
            return false;
        } finally {
            // close here quietly to clean up in all situations
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }

        return copyDone;
    }

    public static boolean copy(final InputStream input, final OutputStream output) {
        try {
            int length;
            final byte[] buffer = new byte[4096];
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            // Flushing is only necessary if the stream is not immediately closed afterwards.
            // We rely on all callers to do that correctly outside of this method
        } catch (final IOException e) {
            Log.e("LocalStorage.copy: error when copying data", e);
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

    public static boolean deleteDirectory(@NonNull final File dir) {
        final File[] files = dir.listFiles();

        // Although we are called on an existing directory, it might have been removed concurrently
        // in the meantime, for example by the user or by another cleanup task.
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    FileUtils.delete(file);
                }
            }
        }

        return FileUtils.delete(dir);
    }

    /**
     * Deletes all files from directory geocode with the given prefix.
     *
     * @param geocode
     *            The geocode identifying the cache directory
     * @param prefix
     *            The filename prefix
     */
    public static void deleteFilesWithPrefix(final String geocode, final String prefix) {
        final File[] filesToDelete = getFilesWithPrefix(geocode, prefix);
        if (filesToDelete == null) {
            return;
        }
        for (final File file : filesToDelete) {
            try {
                if (!FileUtils.delete(file)) {
                    Log.w("LocalStorage.deleteFilesPrefix: Can't delete file " + file.getName());
                }
            } catch (final Exception e) {
                Log.e("LocalStorage.deleteFilesPrefix", e);
            }
        }
    }

    /**
     * Get an array of all files of the geocode directory starting with
     * the given filenamePrefix.
     *
     * @param geocode
     *            The geocode identifying the cache data directory
     * @param filenamePrefix
     *            The prefix of the files
     * @return File[] the array of files starting with filenamePrefix in geocode directory
     */
    public static File[] getFilesWithPrefix(final String geocode, final String filenamePrefix) {
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String filename) {
                return filename.startsWith(filenamePrefix);
            }
        };
        return LocalStorage.getStorageDir(geocode).listFiles(filter);
    }

    /**
     * Get all storages available on the device.
     * Will include paths like /mnt/sdcard /mnt/usbdisk /mnt/ext_card /mnt/sdcard/ext_card
     */
    public static List<File> getStorages() {

        final String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        final List<File> storages = new ArrayList<>();
        storages.add(new File(extStorage));
        final File file = new File(FILE_SYSTEM_TABLE_PATH);
        if (file.canRead()) {
            Reader fr = null;
            BufferedReader br = null;
            try {
                fr = new InputStreamReader(new FileInputStream(file), CharEncoding.UTF_8);
                br = new BufferedReader(fr);
                String s = br.readLine();
                while (s != null) {
                    if (s.startsWith("dev_mount")) {
                        final String[] tokens = StringUtils.split(s);
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
                    s = br.readLine();
                }
            } catch (final IOException e) {
                Log.e("Could not get additional mount points for user content. " +
                        "Proceeding with external storage only (" + extStorage + ")", e);
            } finally {
                IOUtils.closeQuietly(fr);
                IOUtils.closeQuietly(br);
            }
        }
        return storages;
    }
}
