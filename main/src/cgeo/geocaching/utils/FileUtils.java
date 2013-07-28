package cgeo.geocaching.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.List;

/**
 * Utility class for files
 *
 * @author rsudev
 *
 */
public final class FileUtils {

    private FileUtils() {
        // utility class
    }

    public static void listDir(List<File> result, File directory, FileSelector chooser, Handler feedBackHandler) {

        if (directory == null || !directory.isDirectory() || !directory.canRead()
                || result == null
                || chooser == null) {
            return;
        }

        final File[] files = directory.listFiles();

        if (ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
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
    public static File getUniqueNamedFile(final String baseNameAndPath) {
        String extension = StringUtils.substringAfterLast(baseNameAndPath, ".");
        String pathName = StringUtils.substringBeforeLast(baseNameAndPath, ".");
        int number = 1;
        while (new File(getNumberedFileName(pathName, extension, number)).exists()) {
            number++;
        }
        return new File(getNumberedFileName(pathName, extension, number));
    }

    private static String getNumberedFileName(String pathName, String extension, int number) {
        return pathName + (number > 1 ? "_" + Integer.toString(number) : "") + "." + extension;
    }
}
