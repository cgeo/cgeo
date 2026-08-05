package cgeo.geocaching.utils;

import cgeo.geocaching.R;

import android.content.Context;
import android.os.StatFs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for files
 */
public final class FileUtils {

    public static final String HEADER_LAST_MODIFIED = "last-modified";
    public static final String HEADER_ETAG = "etag";

    public static final String GPX_FILE_EXTENSION = ".gpx";
    public static final String LOC_FILE_EXTENSION = ".loc";
    public static final String ZIP_FILE_EXTENSION = ".zip";
    public static final String MAP_FILE_EXTENSION = ".map";
    public static final String BACKGROUND_MAP_FILE_EXTENSION = ".mbtiles";

    private static final String FILE_PROTOCOL = "file://";

    private static final String FORBIDDEN_FILENAME_CHARS_HEX = new String(new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x7f});
    public static final String FORBIDDEN_FILENAME_CHARS = "\"*/:<>?\\|" + FORBIDDEN_FILENAME_CHARS_HEX;

    private FileUtils() {
        // utility class
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
                    delete(file);
                }
            }
        }

        return delete(dir);
    }

    /**
     * Moves a file/directory to a new name. Tries a rename first (faster) and falls back
     * to a copy + delete. Target directories are created if needed.
     *
     * @param source source file or directory
     * @param target target file or directory
     * @return true, if successfully
     */
    public static boolean move(final File source, final File target) {
        if (!source.exists()) {
            return false;
        }
        FileUtils.mkdirs(target.getParentFile());
        boolean success = source.renameTo(target);
        if (!success) {
            // renameTo might fail across mount points, try copy/delete instead
            success = FileUtils.copy(source, target);
            if (success) {
                FileUtils.deleteDirectory(source);
            } else {
                Log.w("Couldn't move " + source + " to " + target);
            }
        }
        return success;
    }

    /**
     * Moves a file/directory into the targetDirectory.
     *
     * @param source          source file or directory
     * @param targetDirectory target directory
     * @return success true or false
     */
    public static boolean moveTo(final File source, final File targetDirectory) {
        return move(source, new File(targetDirectory, source.getName()));
    }

    /**
     * Get the guessed file extension of an URL. A file extension can contain up-to 4 characters in addition to the dot.
     *
     * @param url the relative or absolute URL
     * @return the file extension, including the leading dot, or the empty string if none could be determined
     */
    @NonNull
    public static String getExtension(@NonNull final String url) {
        final String urlExt;
        if (url.startsWith("data:")) {
            // "data:image/png;base64,i53…" -> ".png"
            urlExt = StringUtils.substringAfter(StringUtils.substringBefore(url, ";"), "/");
        } else {
            // "http://example.com/foo/bar.png" -> ".png"
            urlExt = StringUtils.substringAfterLast(url, ".");
        }
        return !urlExt.isEmpty() && urlExt.length() <= 4 ? "." + urlExt : "";
    }

    /**
     * Get the guessed filename from a path
     *
     * @param path filename, optionally including path
     * @return the filename without path
     */
    @NonNull
    public static String getFilenameFromPath(@NonNull final String path) {
        final int posSegment = path.lastIndexOf('/');
        return (posSegment >= 0) ? path.substring(posSegment + 1) : path;
    }

    /**
     * Copy a file into another. The directory structure of target file will be created if needed.
     *
     * @param source      the source file
     * @param destination the target file
     * @return true if the copy happened without error, false otherwise
     */
    public static boolean copy(@NonNull final File source, @NonNull final File destination) {
        try {
            if (source.isDirectory()) {
                org.apache.commons.io.FileUtils.copyDirectory(source, destination);
            } else {
                org.apache.commons.io.FileUtils.copyFile(source, destination);
            }
            return true;
        } catch (final IOException e) {
            Log.w("FileUtils.copy: could not copy file", e);
            return false;
        }
    }

    /**
     * Save a stream to a file.
     * <p/>
     * If the response could not be saved to the file due, for example, to a network error, the file will not exist when
     * this method returns.
     *
     * @param inputStream the stream whose content will be saved
     * @param targetFile  the target file, which will be created if necessary
     * @return true if the operation was successful, false otherwise
     */
    public static boolean saveToFile(@Nullable final InputStream inputStream, @NonNull final File targetFile) {
        if (inputStream == null) {
            return false;
        }

        File tempFile = null;
        try {
            tempFile = File.createTempFile("download", null, targetFile.getParentFile());
            final FileOutputStream fos = new FileOutputStream(tempFile);
            IOUtils.copy(inputStream, fos);
            fos.close();
            return tempFile.renameTo(targetFile);
        } catch (final IOException e) {
            Log.e("FileUtils.saveToFile", e);
            deleteIgnoringFailure(tempFile);
            deleteIgnoringFailure(targetFile);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return false;
    }

    @NonNull
    public static File buildFile(final File base, @NonNull final String fileName, final boolean isUrl, final boolean createDirs) {
        if (createDirs) {
            mkdirs(base);
        }
        return new File(base, isUrl ? CryptUtils.md5(fileName) + getExtension(fileName) : fileName);
    }

    /**
     * Save an HTTP response to a file.
     *
     * @param response   the response whose entity content will be saved
     * @param targetFile the target file, which will be created if necessary
     * @return true if the operation was successful, false otherwise, in which case the file will not exist
     */
    public static boolean saveEntityToFile(@NonNull final Response response, @NonNull final File targetFile) {
        try {
            final boolean saved = saveToFile(response.body().byteStream(), targetFile);
            if (saved) {
                saveHeader(HEADER_ETAG, response, targetFile);
                saveHeader(HEADER_LAST_MODIFIED, response, targetFile);
            }
            return saved;
        } catch (final Exception e) {
            Log.e("FileUtils.saveEntityToFile", e);
        }

        return false;
    }

    private static void saveHeader(final String name, @NonNull final Response response, @NonNull final File baseFile) {
        final String header = response.header(name);
        final File file = filenameForHeader(baseFile, name);
        if (header == null) {
            deleteIgnoringFailure(file);
        } else {
            saveToFile(new ByteArrayInputStream(header.getBytes(StandardCharsets.UTF_8)), file);
        }
    }

    @NonNull
    private static File filenameForHeader(@NonNull final File baseFile, final String name) {
        return new File(baseFile.getAbsolutePath() + "-" + name);
    }

    /**
     * Get the saved header value for this file.
     *
     * @param baseFile the name of the cached resource
     * @param name     the name of the header ("etag" or "last-modified")
     * @return the cached value, or <tt>null</tt> if none has been cached
     */
    @Nullable
    public static String getSavedHeader(@NonNull final File baseFile, final String name) {
        try {
            final File file = filenameForHeader(baseFile, name);
            final Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            try {
                // No header will be more than 256 bytes
                final char[] value = new char[256];
                final int count = reader.read(value);
                return new String(value, 0, count);
            } finally {
                IOUtils.closeQuietly(reader);
            }
        } catch (final FileNotFoundException ignored) {
            // Do nothing, the file does not exist
        } catch (final Exception e) {
            Log.w("could not read saved header " + name + " for " + baseFile, e);
        }
        return null;
    }

    /**
     * Create a unique non existing file named like the given file name. If a file with the given name already exists,
     * add a number as suffix to the file name.<br>
     * Example: For the file name "file.ext" this will return the first file of the list
     * <ul>
     * <li>file.ext</li>
     * <li>file_2.ext</li>
     * <li>file_3.ext</li>
     * </ul>
     * which does not yet exist.
     */
    @NonNull
    public static File getUniqueNamedFile(final File file) {
        if (!file.exists()) {
            return file;
        }
        final String baseNameAndPath = file.getPath();
        final String prefix = StringUtils.substringBeforeLast(baseNameAndPath, ".") + "_";
        final String extension = "." + StringUtils.substringAfterLast(baseNameAndPath, ".");
        for (int i = 2; i < Integer.MAX_VALUE; i++) {
            final File numbered = new File(prefix + i + extension);
            if (!numbered.exists()) {
                return numbered;
            }
        }
        throw new IllegalStateException("Unable to generate a non-existing file name");
    }

    public static String createUniqueFilename(@NonNull final String requestedName, @NonNull final List<String> existingNames) {
        return createUniqueFilename(requestedName, existingNames, null);
    }


    public static String createUniqueFilename(@NonNull final String requestedName, @NonNull final List<String> existingNames, @Nullable final File dir) {

        //split in suffix and praefix
        final int suffIdx = requestedName.lastIndexOf(".");
        final String suffix = suffIdx >= 0 ? requestedName.substring(suffIdx) : "";
        final String praefix = suffIdx >= 0 ? requestedName.substring(0, suffIdx) : requestedName;

        String newPraefix = praefix;
        int idx = 1;
        while (existingNames.contains(newPraefix + suffix) || (dir != null && dir.isDirectory() && new File(dir, newPraefix + suffix).exists())) {
            newPraefix = praefix + " (" + (idx++) + ")";
        }
        return newPraefix + suffix;
    }

    /**
     * This usage of this method indicates that the return value of File.delete() can safely be ignored.
     */
    public static void deleteIgnoringFailure(final File file) {
        final boolean success = file.delete() || !file.exists();
        if (!success) {
            Log.i("Could not delete " + file.getAbsolutePath());
        }
    }

    /**
     * Deletes a file and logs deletion failures.
     *
     * @return {@code true} if this file was deleted, {@code false} otherwise.
     */
    public static boolean delete(final File file) {
        final boolean success = file.delete() || !file.exists();
        if (!success) {
            Log.w("Could not delete " + file.getAbsolutePath());
        }
        return success;
    }

    /**
     * Creates the directory named by the given file, creating any missing parent directories in the process.
     *
     * @return {@code true} if the directory was created, {@code false} on failure or if the directory already
     * existed.
     */
    public static boolean mkdirs(final File file) {
        final boolean success = file.mkdirs() || file.isDirectory(); // mkdirs returns false on existing directories
        if (!success) {
            Log.w("Could not make directories " + file.getAbsolutePath());
        }
        return success;
    }

    /**
     * Check if the URL represents a file on the local file system.
     *
     * @return <tt>true</tt> if the URL scheme is <tt>file</tt>, <tt>false</tt> otherwise
     */
    public static boolean isFileUrl(final String url) {
        return StringUtils.startsWith(url, FILE_PROTOCOL);
    }

    /**
     * Build an URL from a file name.
     *
     * @param file a local file name
     * @return an URL with the <tt>file</tt> scheme
     */
    @NonNull
    public static String fileToUrl(final File file) {
        return FILE_PROTOCOL + file.getAbsolutePath();
    }

    /**
     * Local file name when {@link #isFileUrl(String)} is <tt>true</tt>.
     *
     * @return the local file
     */
    @NonNull
    public static File urlToFile(final String url) {
        return new File(StringUtils.substring(url, FILE_PROTOCOL.length()));
    }

    /**
     * Returns the size in bytes of a file or directory.
     */
    public static long getSize(final File file) {
        if (file == null || !file.exists()) {
            return 0;
        }
        if (file.isDirectory()) {
            long result = 0;
            final File[] fileList = file.listFiles();
            if (ArrayUtils.isNotEmpty(fileList)) {
                for (final File aFileList : fileList) {
                    result += getSize(aFileList);
                }
            }
            return result; // return the file size
        }
        return file.length();
    }

    /**
     * Returns the available space in bytes on the mount point used by the given dir.
     */
    public static long getFreeDiskSpace(final File dir) {
        if (dir == null) {
            return 0;
        }
        try {
            final StatFs statFs = new StatFs(dir.getAbsolutePath());
            return statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        } catch (final IllegalArgumentException ignored) {
            // thrown if the directory isn't pointing to an external storage
        }
        return 0;
    }

    public static String getRawResourceAsString(final Context context, @RawRes final int resId) {
        final StringBuilder content = new StringBuilder();
        try (BufferedReader is = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resId)))) {
            String line;
            while ((line = is.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException ignored) {
        }
        return content.toString();
    }

    public static String getChangelogMaster(final Context context) {
        return getRawResourceAsString(context, R.raw.changelog_base);
    }

    public static String getChangelogRelease(final Context context) {
        return getRawResourceAsString(context, R.raw.changelog_bugfix);
    }

    @NonNull
    public static String replaceNonFilenameChars(@Nullable final String text) {
        if (StringUtils.isBlank(text)) {
            return "-";
        }
        return text.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    @Nullable
    public static File getOrCreate(@NonNull final File dir, @Nullable final String prefix, @Nullable final String suffix, @Nullable final byte[] data) {
        if (data == null) {
            return null;
        }
        mkdirs(dir);
        final File file = new File(dir, createFilenameFor(prefix, suffix, data));
        if (!file.isFile()) {
            try {
                org.apache.commons.io.FileUtils.writeByteArrayToFile(file, data);
            } catch (Exception e) {
                Log.e("Problem extracting/storing data (prefix='" + prefix + "', dir = '" + dir + "'", e);
                return null;
            }
        }
        return file;
    }

    private static String createFilenameFor(@Nullable final String prefix, @Nullable final String suffix, @Nullable final byte[] data) {
        final StringBuilder sb = new StringBuilder();
        if (data == null) {
            sb.append("null");
        } else if (data.length == 0 || data.length == 1) {
            sb.append(data.length == 0 ? "empty" : data[0]);
        } else {
            sb.append(data.length).append("_").append(Arrays.hashCode(data))
                .append("_").append(data[1]).append("_").append(data[data.length - 2]);
        }
        return (StringUtils.isBlank(prefix) ? "" : replaceNonFilenameChars(prefix) + "_") + sb + (StringUtils.isBlank(suffix) ? "" : "." + replaceNonFilenameChars(suffix));
    }

}
