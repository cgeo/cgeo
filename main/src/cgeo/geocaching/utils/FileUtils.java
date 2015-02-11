package cgeo.geocaching.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * Utility class for files
 *
 */
public final class FileUtils {

    private static final String FILE_PROTOCOL = "file://";

    private FileUtils() {
        // utility class
    }

    public static void listDir(final List<File> result, final File directory, final FileSelector chooser, final Handler feedBackHandler) {

        if (directory == null || !directory.isDirectory() || !directory.canRead()
                || result == null
                || chooser == null) {
            return;
        }

        final File[] files = directory.listFiles();

        if (ArrayUtils.isNotEmpty(files)) {
            for (final File file : files) {
                if (chooser.shouldEnd()) {
                    return;
                }
                if (!file.canRead()) {
                    continue;
                }
                String name = file.getName();
                if (file.isFile()) {
                    if (chooser.isSelected(file)) {
                        result.add(file); // add file to list
                    }
                } else if (file.isDirectory()) {
                    if (name.charAt(0) == '.') {
                        continue; // skip hidden directories
                    }
                    if (name.length() > 16) {
                        name = name.substring(0, 14) + '…';
                    }
                    if (feedBackHandler != null) {
                        feedBackHandler.sendMessage(Message.obtain(feedBackHandler, 0, name));
                    }

                    listDir(result, file, chooser, feedBackHandler); // go deeper
                }
            }
        }
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

    public static abstract class FileSelector {
        public abstract boolean isSelected(File file);

        public abstract boolean shouldEnd();
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
    public static File getUniqueNamedFile(final File file) {
        if (!file.exists()) {
            return file;
        }
        final String baseNameAndPath = file.getPath();
        final String prefix = StringUtils.substringBeforeLast(baseNameAndPath, ".") + "_";
        final String extension = "." + StringUtils.substringAfterLast(baseNameAndPath, ".");
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            final File numbered = new File(prefix + i + extension);
            if (!numbered.exists()) {
                return numbered;
            }
        }
        throw new IllegalStateException("Unable to generate a non-existing file name");
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
     * @return <code> true</code> if this file was deleted, <code>false</code> otherwise.
     */
    public static boolean delete(final File file) {
        final boolean success = file.delete() || !file.exists();
        if (!success) {
            Log.e("Could not delete " + file.getAbsolutePath());
        }
        return success;
    }

    /**
     * Creates the directory named by the given file, creating any missing parent directories in the process.
     *
     * @return <code>true</code> if the directory was created, <code>false</code> on failure or if the directory already
     *         existed.
     */
    public static boolean mkdirs(final File file) {
        final boolean success = file.mkdirs() || file.isDirectory(); // mkdirs returns false on existing directories
        if (!success) {
            Log.e("Could not make directories " + file.getAbsolutePath());
        }
        return success;
    }

    public static boolean writeFileUTF16(final File file, final String content) {
        // TODO: replace by some apache.commons IOUtils or FileUtils code
        Writer fileWriter = null;
        BufferedOutputStream buffer = null;
        try {
            final OutputStream os = new FileOutputStream(file);
            buffer = new BufferedOutputStream(os);
            fileWriter = new OutputStreamWriter(buffer, CharEncoding.UTF_16);
            fileWriter.write(content);
        } catch (final IOException e) {
            Log.e("FieldnoteExport.ExportTask export", e);
            return false;
        } finally {
            IOUtils.closeQuietly(fileWriter);
            IOUtils.closeQuietly(buffer);
        }
        return true;
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
    public static String fileToUrl(final File file) {
        return FILE_PROTOCOL + file.getAbsolutePath();
    }

    /**
     * Local file name when {@link #isFileUrl(String)} is <tt>true</tt>.
     * 
     * @return the local file
     */
    public static File urlToFile(final String url) {
        return new File(StringUtils.substring(url, FILE_PROTOCOL.length()));
    }
}
