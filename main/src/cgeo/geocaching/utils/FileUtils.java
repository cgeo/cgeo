package cgeo.geocaching.utils;

import org.apache.commons.lang3.ArrayUtils;

import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.List;

/**
 * Utiliy class for files
 *
 * @author rsudev
 *
 */
public class FileUtils {

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
}
